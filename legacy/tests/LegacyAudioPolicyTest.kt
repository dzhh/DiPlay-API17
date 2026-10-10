package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
class LegacyAudioPolicyTest {
 @Test fun assistantUsesMediaOutput(){assertEquals(3,LegacyAudioPolicy.stream("assistant",10));assertEquals(3,LegacyAudioPolicy.stream("assistant",-1));assertEquals(3,LegacyAudioPolicy.stream("assistant",14))}
 @Test fun callAndAssistantNeverBecomeNavigation(){assertEquals("call",LegacyAudioPolicy.role(100,"telephony"));assertEquals("assistant",LegacyAudioPolicy.role(100,"speechRecognition"));assertEquals(3,LegacyAudioPolicy.stream("call",10))}
 @Test fun navigationOnlyUsesAuditedSecondOutput(){assertEquals(10,LegacyAudioPolicy.stream("navigation",10));assertEquals(3,LegacyAudioPolicy.stream("media",10));assertEquals(3,LegacyAudioPolicy.stream("navigation",14));assertEquals(3,LegacyAudioPolicy.stream("navigation",-1))}
 @Test fun lowLatencyMediaIsStillMedia(){assertEquals("media",LegacyAudioPolicy.role(100,"media"));assertEquals("media",LegacyAudioPolicy.role(102,"default"));assertEquals("navigation",LegacyAudioPolicy.role(100,"default"));assertEquals("navigation",LegacyAudioPolicy.role(100,"alert"))}
}
