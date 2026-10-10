package local.headunit.diplay
import com.shilapi.xcertplay.airplay.AudioStream
import java.net.*
import com.shilapi.xcertplay.airplay.AirPlayCrypto
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test

class AudioStreamLifecycleTest {
 private val address=InetAddress.getByName("127.0.0.1")
 private fun closeAndJoin(stream:AudioStream){stream.close();for(name in listOf("dataThread","controlThread")){val f=AudioStream::class.java.getDeclaredField(name);f.isAccessible=true;val t=f.get(stream) as? Thread?:continue;t.join(1500);assertFalse("Receiver leaked: $name",t.isAlive)}}
 @Test fun failedStartupClosesAlreadyBoundSocket(){val a=AudioStream(ByteArray(32),100,{throw IllegalStateException("diagnostic failure")},address);try{a.listen(object:AudioStream.Listener{});fail()}catch(expected:IllegalStateException){};val f=AudioStream::class.java.getDeclaredField("dataSocket");f.isAccessible=true;assertTrue((f.get(a) as DatagramSocket).isClosed);a.close()}
 @Test fun closedStreamCannotListen(){val a=AudioStream(ByteArray(32),100,{},address);a.close();try{a.listen(object:AudioStream.Listener{});fail()}catch(expected:IllegalStateException){}}
 @Test fun repeatedCloseReleasesBothUdpPorts(){repeat(20){val a=AudioStream(ByteArray(32),100,{},address);val ports=a.listen(object:AudioStream.Listener{});a.close();a.close();for(port in listOf(ports.first,ports.second)){DatagramSocket(null).use{it.reuseAddress=false;it.bind(InetSocketAddress(address,port))}};for(name in listOf("dataThread","controlThread")){val f=AudioStream::class.java.getDeclaredField(name);f.isAccessible=true;val t=f.get(a) as Thread;t.join(1000);assertFalse("Leaked $name",t.isAlive)}}}
 @Test fun largerEncryptedPcmDatagramIsNotTruncated(){val key=ByteArray(32);val payload=ByteArray(5000){it.toByte()};val header=ByteArray(12);val wire=header+AirPlayCrypto.chachaSeal(key,ByteArray(12),payload,header.copyOfRange(4,12))+ByteArray(8);val received=CountDownLatch(1);val data=AtomicReference<ByteArray>();val error=AtomicReference<Throwable>();val stream=AudioStream(key,100,{},address);val ports=stream.listen(object:AudioStream.Listener{override fun onRtp(rtp:ByteArray,sample:Int){data.set(rtp);received.countDown()};override fun onPacket(wire:ByteArray,rtp:ByteArray?,sample:Int?,failure:Throwable?){if(failure!=null){error.set(failure);received.countDown()}}})
  try{DatagramSocket().use{it.send(DatagramPacket(wire,wire.size,address,ports.first))};assertTrue(received.await(2,TimeUnit.SECONDS));assertNull(error.get());assertArrayEquals(header+payload,data.get())}finally{closeAndJoin(stream)}
 }
 @Test fun receiverCallbackFailureClosesStreamAndNotifiesOwner(){val closed=CountDownLatch(1);val stream=AudioStream(ByteArray(32),100,{},address);val error=AtomicReference<Throwable>();val ports=stream.listen(object:AudioStream.Listener{override fun onPacket(wire:ByteArray,rtp:ByteArray?,sample:Int?,failure:Throwable?){throw IllegalStateException("sink failed")};override fun onClosed(cause:Throwable){error.set(cause);closed.countDown()}})
  try{DatagramSocket().use{it.send(DatagramPacket(byteArrayOf(0),1,address,ports.first))};assertTrue(closed.await(2,TimeUnit.SECONDS));assertTrue(error.get() is IllegalStateException);for(port in listOf(ports.first,ports.second))DatagramSocket(null).use{it.bind(InetSocketAddress(address,port))}}finally{closeAndJoin(stream)}
 }
}
