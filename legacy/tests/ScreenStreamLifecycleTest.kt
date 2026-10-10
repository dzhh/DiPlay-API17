package local.headunit.diplay
import com.shilapi.xcertplay.airplay.ScreenStream
import java.net.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class ScreenStreamLifecycleTest {
 private val address=InetAddress.getByName("127.0.0.1")
 @Test fun closeBeforeListenCannotLeakListener(){val stream=ScreenStream(ByteArray(32),{},address);stream.close();try{stream.listen(object:ScreenStream.Listener{});fail()}catch(expected:IllegalStateException){}}
 @Test fun closeRacingAcceptReleasesEverySocket(){repeat(40){val s=ScreenStream(ByteArray(32),{},address);val port=s.listen(object:ScreenStream.Listener{});val client=Socket(address,port);s.close();client.soTimeout=1000;try{assertEquals(-1,client.getInputStream().read())}catch(expected:java.net.SocketException){}finally{client.close()};val field=ScreenStream::class.java.getDeclaredField("thread");field.isAccessible=true;val t=field.get(s) as Thread;t.join(1500);assertFalse("Screen receiver retained after close",t.isAlive);ServerSocket().use{it.reuseAddress=true;it.bind(InetSocketAddress(address,port))}}}
 @Test fun peerEofClosesListenerAndNotifiesExactlyOnceEvenIfCallbackThrows(){val count=AtomicInteger();val done=CountDownLatch(1);val stream=ScreenStream(ByteArray(32),{},address);val port=stream.listen(object:ScreenStream.Listener{override fun onClosed(cause:Throwable?){count.incrementAndGet();done.countDown();throw IllegalStateException("callback failure")}});Socket(address,port).close();assertTrue(done.await(2,TimeUnit.SECONDS));val field=ScreenStream::class.java.getDeclaredField("thread");field.isAccessible=true;(field.get(stream) as Thread).join(1500);assertEquals(1,count.get());ServerSocket().use{it.reuseAddress=true;it.bind(InetSocketAddress(address,port))};stream.close()}
}
