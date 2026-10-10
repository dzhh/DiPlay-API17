package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.*

class AudioDeliveryGateTest {
 @Test fun lateCallbacksCannotRecreateOutput(){val events=mutableListOf<String>();var alive=true;val g=AudioDeliveryGate({alive},{events.add("start")},{_,_->events.add("packet")},{events.add("stop")});g.onPacket(byteArrayOf(),0);g.onStarted(0);g.onStarted(1);g.onPacket(byteArrayOf(),0);g.close();g.onStarted(2);g.onPacket(byteArrayOf(),0);g.close();assertEquals(listOf("start","packet","stop"),events)}
 @Test fun retiredOwnerCannotStart(){var starts=0;val g=AudioDeliveryGate({false},{starts++},{_,_->fail()},{fail()});g.onStarted(0);g.close();assertEquals(0,starts)}
 @Test fun partialStartStillGetsCleanup(){var stopped=0;val g=AudioDeliveryGate({true},{throw IllegalStateException("partial start")},{_,_->},{stopped++});try{g.onStarted(0);fail()}catch(expected:IllegalStateException){};g.close();g.close();assertEquals(1,stopped)}
 @Test fun closeWaitsForOwnInflightDelivery(){val entered=CountDownLatch(1);val release=CountDownLatch(1);val done=CountDownLatch(1);val events=java.util.Collections.synchronizedList(mutableListOf<String>());val g=AudioDeliveryGate({true},{},{_,_->entered.countDown();check(release.await(3,TimeUnit.SECONDS));events.add("packet")},{events.add("stop")});g.onStarted(0);val rx=Thread{g.onPacket(byteArrayOf(),0)};rx.start();assertTrue(entered.await(2,TimeUnit.SECONDS));val close=Thread{g.close();done.countDown()};close.start();try{assertFalse(done.await(100,TimeUnit.MILLISECONDS))}finally{release.countDown()};assertTrue(done.await(2,TimeUnit.SECONDS));rx.join(2000);close.join(2000);assertEquals(listOf("packet","stop"),events)}
}
