package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
class FrameRateMeterTest {
 @Test fun measuresFramesOverActualElapsedTime(){val m=FrameRateMeter();assertTrue(m.sample(10,1000,"a").isNaN());assertEquals(20.0,m.sample(40,2500,"a"),0.001)}
 @Test fun stillImageReportsZeroNotConfiguredMaximum(){val m=FrameRateMeter();m.sample(10,1000,"a");assertEquals(0.0,m.sample(10,2000,"a"),0.001)}
 @Test fun reconnectAndCounterResetRebase(){val m=FrameRateMeter();m.sample(100,1000,"a");assertTrue(m.sample(3,2000,"b").isNaN());assertEquals(30.0,m.sample(33,3000,"b"),0.001);assertTrue(m.sample(1,4000,"b").isNaN())}
 @Test fun pauseAndMissingSessionDoNotFabricateRates(){val m=FrameRateMeter();m.sample(10,1000,"a");m.reset();assertTrue(m.sample(500,100000,"a").isNaN());assertTrue(m.sample(600,100000,"a").isNaN());assertTrue(m.sample(0,101000,"").isNaN())}
}
