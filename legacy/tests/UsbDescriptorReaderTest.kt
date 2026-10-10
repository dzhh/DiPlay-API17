package local.headunit.diplay

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class UsbDescriptorReaderTest {
 private fun configuration(id:Int)=byteArrayOf(9,2,9,0,0,id.toByte(),0,0x80.toByte(),50)
 @Test fun readsAllConfigurationsWithoutChangingRequestOrder(){
  val requests=ArrayList<Pair<Int,Int>>()
  val result=UsbDescriptorReader.read {value,bytes->
   requests.add(value to bytes.size)
   if(value==0x100)bytes[17]=2 else configuration(value-0x200+1).copyInto(bytes)
   bytes.size
  }
  assertEquals(listOf(1,2),result.map{it.id})
  assertEquals(listOf(0x100 to 18,0x200 to 9,0x200 to 9,0x201 to 9,0x201 to 9),requests)
 }
 @Test fun cancellationDuringNativeQueryStopsBeforeNextQuery(){
  val entered=CountDownLatch(1);val release=CountDownLatch(1);val outcome=AtomicReference<Throwable>()
  var queries=0
  val worker=Thread {
   try{UsbDescriptorReader.read {_,bytes->
    queries++;entered.countDown()
    // Model a native call that finishes only on its own timeout/completion.
    var interrupted=false
    while(true){try{release.await();break}catch(_:InterruptedException){interrupted=true}}
    if(interrupted)Thread.currentThread().interrupt()
    bytes[17]=16;bytes.size
   };outcome.set(AssertionError("Cancelled read completed"))}catch(t:Throwable){outcome.set(t)}
  }
  worker.start()
  try{assertTrue(entered.await(2,TimeUnit.SECONDS));worker.interrupt()}
  finally{release.countDown();worker.join(2000)}
  assertFalse(worker.isAlive);assertTrue(outcome.get() is InterruptedException);assertEquals(1,queries)
 }
 @Test fun alreadyCancelledReadDoesNotQueryDevice(){
  var queries=0
  try{Thread.currentThread().interrupt();try{UsbDescriptorReader.read{_,_->queries++;0};fail("Read ignored cancellation")}catch(_:InterruptedException){assertTrue(Thread.currentThread().isInterrupted)}}
  finally{Thread.interrupted()}
  assertEquals(0,queries)
 }
 @Test fun incompleteReadDoesNotContinueToConfigurationQueries(){
  var queries=0
  try{UsbDescriptorReader.read{_,_->queries++;17};fail("Accepted truncated device header")}catch(_:IllegalStateException){}
  assertEquals(1,queries)
 }
}
