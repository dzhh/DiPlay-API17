package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
class BydAudioPolicyTest {
 @Test fun matchesCollectedFirmware(){assertTrue(BydAudioPolicy.supported(17,"MX9A0",182));assertTrue(BydAudioPolicy.supported(17,"Freescale MX9A0",182))}
 @Test fun rejectsOtherFirmware(){assertFalse(BydAudioPolicy.supported(17,"MX9A0",183));assertFalse(BydAudioPolicy.supported(28,"MX9A0",182));assertFalse(BydAudioPolicy.supported(17,"other",182))}
}
