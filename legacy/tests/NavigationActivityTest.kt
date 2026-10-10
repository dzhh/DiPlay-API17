package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
class NavigationActivityTest {
 @Test fun silentKeepaliveDoesNotHoldCarNavigationMix(){val a=NavigationActivity();assertFalse(a.packet(ByteArray(320),100));assertTrue(a.packet(byteArrayOf(1,0),1000));assertTrue(a.packet(ByteArray(320),499_999_999));assertFalse(a.packet(ByteArray(320),500_001_000));assertFalse(a.active(1_000_000_000));assertTrue(a.packet(byteArrayOf(0,-1),1_000_000_001))}
 @Test fun timeWithoutPacketsAlsoReleasesMix(){val a=NavigationActivity();assertTrue(a.packet(byteArrayOf(1),10));assertTrue(a.active(499_999_999));assertFalse(a.active(500_000_010));assertFalse(a.packet(byteArrayOf(),500_000_011))}
}
