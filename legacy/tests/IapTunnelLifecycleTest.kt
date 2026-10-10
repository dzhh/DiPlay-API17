package local.headunit.diplay
import com.shilapi.xcertplay.airplay.*
import java.net.*
import java.io.IOException
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test

class IapTunnelLifecycleTest {
 private val address=InetAddress.getByName("127.0.0.1")
 @Test fun closingUnconnectedTunnelDoesNotReportSuccessfulPeer(){val t=IapTunnel(ByteArray(32),address);t.listen(object:IapTunnel.Listener{});t.close();assertFalse(t.awaitPeerConnection(100))}
 @Test fun listenerSetupFailureReleasesBoundPort(){var port=0;val t=IapTunnel(ByteArray(32),address)
  try{t.listen(object:IapTunnel.Listener{override fun onDebug(message:String){if(message.contains("listener bound=")){port=message.substringAfterLast(':').toInt();throw IllegalStateException("test callback failure")}}});fail("Expected setup failure")}catch(expected:IllegalStateException){}
  assertTrue(port>0);ServerSocket().use{it.reuseAddress=true;it.bind(InetSocketAddress("0.0.0.0",port))};assertFalse(t.awaitPeerConnection(0));t.close()
 }
 @Test fun malformedAuthenticatedPackageFailsInsteadOfGrowingBufferForever(){
  val key=ByteArray(32);val error=AtomicReference<Throwable?>();val failed=CountDownLatch(1);val t=IapTunnel(key,address)
  val port=t.listen(object:IapTunnel.Listener{override fun onClosed(cause:Throwable?){error.set(cause);failed.countDown()}})
  try{Socket(address,port).use{s->assertTrue(t.awaitPeerConnection(1000));val payload=ByteArray(32);payload[3]=31;val header=byteArrayOf(32,0);val wire=header+AirPlayCrypto.chachaSeal(key,AirPlayCrypto.nonce64(0),payload,header);s.getOutputStream().write(wire);s.getOutputStream().flush();assertTrue(failed.await(3,TimeUnit.SECONDS));assertTrue(error.get() is IOException);assertTrue(error.get()!!.message!!.contains("Invalid iAP package length"))}}finally{t.close()}
 }
 @Test fun repeatedOpenAndCloseUnblocksPeerWaitAndReleasesPort(){repeat(20){val t=IapTunnel(ByteArray(32),address);val port=t.listen(object:IapTunnel.Listener{});Socket(address,port).use{assertTrue(t.awaitPeerConnection(1000));t.close();assertFalse(t.awaitPeerConnection(0))};ServerSocket().use{it.reuseAddress=true;it.bind(InetSocketAddress("0.0.0.0",port))}}}
}
