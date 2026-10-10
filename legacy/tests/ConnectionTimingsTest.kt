package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
class ConnectionTimingsTest {
 @Test fun separatesPermissionProtocolAndFirstFrame(){val t=ConnectionTimings(100);t.advance("usb_permission",150);t.advance("handshake",1150);t.firstFrame(1400);assertEquals(mapOf("start" to 50L,"usb_permission" to 1000L,"handshake" to 250L,"totalMs" to 1300L),t.snapshot(9000))}
 @Test fun failedStageRetainsElapsedTime(){val t=ConnectionTimings(100);t.advance("tls",200);assertEquals(500L,t.snapshot(700)["tls"]);assertEquals(600L,t.snapshot(700)["totalMs"])}
 @Test fun duplicateFrameAndLateStageCannotExtendStartup(){val t=ConnectionTimings(0);t.firstFrame(250);t.firstFrame(600);t.advance("late",900);assertEquals(mapOf("start" to 250L,"totalMs" to 250L),t.snapshot(3000))}
 @Test fun snapshotsDoNotMutateSubsequentDurations(){val t=ConnectionTimings(0);t.advance("usb",100);t.snapshot(300);t.advance("usb",400);t.firstFrame(600);assertEquals(500L,t.snapshot(1000)["usb"])}
}
