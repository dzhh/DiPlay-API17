package local.headunit.diplay
import java.util.concurrent.*
import org.junit.Assert.*
import org.junit.Test
class TouchDispatchTest {
 @Test fun movesCollapseWithoutCrossingGestureBoundaries(){
  val queue=LinkedBlockingDeque<Runnable>(16);val executor=ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,queue)
  val blocked=CountDownLatch(1);val release=CountDownLatch(1);val values=java.util.Collections.synchronizedList(mutableListOf<Int>())
  executor.execute{blocked.countDown();release.await(3,TimeUnit.SECONDS)}
  try{assertTrue(blocked.await(1,TimeUnit.SECONDS));TouchDispatch.submit(executor,queue,0){values.add(0)}
   repeat(100){n->TouchDispatch.submit(executor,queue,2){values.add(n+1)}}
   TouchDispatch.submit(executor,queue,1){values.add(101)};TouchDispatch.submit(executor,queue,0){values.add(102)}
   TouchDispatch.submit(executor,queue,2){values.add(103)};TouchDispatch.submit(executor,queue,3){values.add(104)}
   assertEquals(6,queue.size);release.countDown();executor.shutdown();assertTrue(executor.awaitTermination(3,TimeUnit.SECONDS));assertEquals(listOf(0,100,101,102,103,104),values)
  }finally{release.countDown();executor.shutdownNow()}
 }
 @Test fun stoppedExecutorRejectsLateGesture(){val q=LinkedBlockingDeque<Runnable>(16);val e=ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,q);e.shutdown();TouchDispatch.submit(e,q,0){fail("late gesture executed")};assertTrue(q.isEmpty())}
}
