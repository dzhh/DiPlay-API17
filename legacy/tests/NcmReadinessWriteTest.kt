package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test

class NcmReadinessWriteTest {
 @Test fun phoneBecomingReadyAfterOldRetryWindowIsReached(){
  var clock=0L;var calls=0
  val result=StartupUsbWrite.awaitReady({clock+=it;if(++calls==24)90 else -110},{},{true},{clock},{clock+=25})
  assertEquals(90,result);assertEquals(24,calls);assertTrue(clock in 4000..4500)
 }
 @Test fun incomingTrafficYieldsToQueuedNeighborReply(){
  var inbound=false;var calls=0
  val result=StartupUsbWrite.awaitReady({calls++;inbound=true;-110},{},{!inbound},{0L},{fail("must yield without sleeping")})
  assertEquals(-110,result);assertEquals(1,calls)
 }
 @Test fun inboundDuringPausePreventsAnotherWrite(){
  var inbound=false;var calls=0
  StartupUsbWrite.awaitReady({calls++;-110},{},{!inbound},{0L},{inbound=true})
  assertEquals(1,calls)
 }
 @Test fun permanentErrorsAndPartialOrZeroWritesAreNeverRetried(){
  for(value in listOf(-19,-108,-32,0,40,90)){
   var calls=0;assertEquals(value,StartupUsbWrite.awaitReady({calls++;value},{},{true},{0L},{fail()}));assertEquals(1,calls)
  }
 }
 @Test fun totalWaitIsBoundedAndLastIoUsesRemainingBudget(){
  var clock=0L;val budgets=ArrayList<Int>()
  assertEquals(-110,StartupUsbWrite.awaitReady({budgets.add(it);clock+=it;-110},{},{true},{clock},{clock+=25}))
  assertTrue(clock in 15000..15025);assertTrue(budgets.size<100);assertTrue(budgets.last()<150)
 }
 @Test fun cancellationStopsBeforeNextIo(){
  var checks=0;var calls=0
  try{StartupUsbWrite.awaitReady({calls++;-110},{if(++checks==2)throw IllegalStateException("closed")},{true},{0L},{ });fail()}
  catch(_:IllegalStateException){};assertEquals(1,calls)
 }
 @Test fun immediateAgainCannotSpinWithoutBound(){
  var calls=0;var sleeps=0
  StartupUsbWrite.awaitReady({calls++;-11},{},{true},{0L},{sleeps++})
  assertEquals(128,calls);assertEquals(128,sleeps)
 }
}
