package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
class SiriCapturePolicyTest {
 private fun event(mode:Any)=mapOf("appStates" to listOf(mapOf("appStateID" to 1L,"speechMode" to mode)))
 @Test fun speakListenSpeakKeepsCaptureOnlyForListening(){val p=SiriCapturePolicy();assertTrue(p.capture(true,"speechrecognition"));assertTrue(p.update(event(1L)));assertFalse(p.capture(true,"speechrecognition"));assertFalse(p.update(event(1)));assertTrue(p.update(event(2)));assertTrue(p.capture(true,"speechrecognition"));assertTrue(p.update(event(1)));assertFalse(p.capture(true,"speechrecognition"));assertTrue(p.update(event(-1)));assertFalse(p.capture(true,"speechrecognition"))}
 @Test fun phonesAndOtherFirmwareKeepDuplex(){val p=SiriCapturePolicy();p.update(event(1));assertTrue(p.capture(true,"telephony"));assertTrue(p.capture(false,"speechrecognition"))}
 @Test fun malformedOrUnrelatedModesCannotReopenMicrophone(){
  val p=SiriCapturePolicy();p.update(event(1))
  for(v in listOf<Any>(2.5,"2",0,9)){assertFalse(p.update(event(v)));assertFalse(p.capture(true,"speechrecognition"))}
  val unrelated=mapOf("appStates" to listOf(mapOf("appStateID" to 2,"speechMode" to 2)))
  assertFalse(p.update(unrelated));assertFalse(p.update(emptyMap()));assertFalse(p.capture(true,"speechrecognition"))
 }
}
