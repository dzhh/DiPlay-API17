package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
class MediaWorkersTest {
 @Test fun busyUntilFinallyCompletes(){val started=CountDownLatch(1);val release=CountDownLatch(1);val t=MediaWorkers.start("release-test"){started.countDown();release.await()};started.await();try{assertTrue(MediaWorkers.busy());assertFalse(MediaWorkers.await(t,10));assertTrue(MediaWorkers.busy())}finally{release.countDown();assertTrue(MediaWorkers.await(t))};assertFalse(MediaWorkers.busy())}
 @Test fun exceptionReleasesCount(){val old=Thread.getDefaultUncaughtExceptionHandler();Thread.setDefaultUncaughtExceptionHandler{_,_->};try{val t=MediaWorkers.start("failure-test"){throw IllegalStateException("expected")};assertTrue(MediaWorkers.await(t));assertFalse(MediaWorkers.busy())}finally{Thread.setDefaultUncaughtExceptionHandler(old)}}
}
