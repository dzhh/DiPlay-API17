package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
class RecorderNativeStartTest {
 class Recorder {
  private var mState=1;private var mRecordingState=1;private val mRecordingStateLock=Any();private var mStart=false
  var calls=0;var code=0
  private fun native_start(event:Int,session:Int):Int {check(Thread.holdsLock(mRecordingStateLock));check(event==0&&session==0);calls++;return code}
  private fun native_stop(){}
  fun recording()=mRecordingState
  fun ownsGlobalFocus()=mStart
 }
 @Test fun preservesStateAndNeverAcquiresGlobalHook(){val r=Recorder();val n=RecorderNativeStart(Recorder::class.java);assertTrue(n.hasVendorHook());n.start(r);assertEquals(1,r.calls);assertEquals(3,r.recording());assertFalse(r.ownsGlobalFocus())}
 @Test fun failedNativeStartNeverClaimsRecording(){val r=Recorder();r.code=-3;try{RecorderNativeStart(Recorder::class.java).start(r);fail()}catch(expected:IllegalStateException){};assertEquals(1,r.calls);assertEquals(1,r.recording())}
 @Test fun cannotStartTwice(){val r=Recorder();val n=RecorderNativeStart(Recorder::class.java);n.start(r);try{n.start(r);fail()}catch(expected:IllegalStateException){};assertEquals(1,r.calls)}
 @Test(expected=NoSuchFieldException::class) fun unknownLayoutRejectedBeforeNativeCall(){RecorderNativeStart(String::class.java)}
}
