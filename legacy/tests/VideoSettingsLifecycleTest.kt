package local.headunit.diplay
import com.shilapi.xcertplay.airplay.*
import java.net.*
import java.io.DataInputStream
import org.junit.Assert.*
import org.junit.Test

class VideoSettingsLifecycleTest {
 private val address=InetAddress.getByName("127.0.0.1")
 private fun join(channel:VideoSettingsChannel){val field=VideoSettingsChannel::class.java.getDeclaredField("thread");field.isAccessible=true;val thread=field.get(channel) as Thread;thread.join(1500);assertFalse("Settings receiver leaked",thread.isAlive)}
 @Test fun closedChannelCannotReopen(){val c=VideoSettingsChannel(ByteArray(32),ByteArray(32),{});c.close();try{c.listen(address);fail()}catch(expected:IllegalStateException){}}
 @Test fun acceptRacingCloseReleasesSocketAndListener(){repeat(25){val c=VideoSettingsChannel(ByteArray(32),ByteArray(32),{});val port=c.listen(address);Socket(address,port).use{s->s.soTimeout=1000;c.close();try{assertEquals(-1,s.getInputStream().read())}catch(expected:SocketException){}};join(c);ServerSocket().use{it.reuseAddress=true;it.bind(InetSocketAddress(address,port))}}}
 @Test fun fragmentedEncryptedSyncReturnsAuthenticatedReply(){val key=ByteArray(32){it.toByte()};val c=VideoSettingsChannel(key,key,{});val port=c.listen(address)
  try{Socket(address,port).use{s->s.soTimeout=1500;val message=ByteArray(32);message[3]=32;"sync".toByteArray().copyInto(message,4);val sequence=byteArrayOf(1,2,3,4,5,6,7,8);sequence.copyInto(message,20);val header=byteArrayOf(32,0);val wire=header+AirPlayCrypto.chachaSeal(key,AirPlayCrypto.nonce64(0),message,header);val out=s.getOutputStream();out.write(wire,0,1);out.flush();out.write(wire,1,wire.size-1);out.flush();val input=DataInputStream(s.getInputStream());val aad=ByteArray(2);input.readFully(aad);val length=(aad[0].toInt() and 255) or ((aad[1].toInt() and 255) shl 8);assertEquals(32,length);val encrypted=ByteArray(length+16);input.readFully(encrypted);val reply=AirPlayCrypto.chachaOpen(key,AirPlayCrypto.nonce64(0),encrypted,aad);assertEquals("rply",String(reply,4,4));assertArrayEquals(sequence,reply.copyOfRange(20,28))}}finally{c.close();join(c)}
 }
 @Test fun diagnosticFailureStillClosesAllSockets(){val c=VideoSettingsChannel(ByteArray(32),ByteArray(32),{throw IllegalStateException("callback")});val port=c.listen(address);Socket(address,port).use{s->s.soTimeout=1000;try{assertEquals(-1,s.getInputStream().read())}catch(expected:SocketException){}};join(c);ServerSocket().use{it.reuseAddress=true;it.bind(InetSocketAddress(address,port))}}
}
