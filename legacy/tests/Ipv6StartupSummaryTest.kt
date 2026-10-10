package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
class Ipv6StartupSummaryTest {
 @Test fun capturesSynWithoutPayload(){val p=ByteArray(80);p[0]=0x60;p[5]=40;p[6]=6;p[40]=0x12;p[41]=0x34;p[42]=0x1b;p[43]=0x58;p[47]=42;p[53]=2;"PRIVATE_PAYLOAD".toByteArray().copyInto(p,60);val s=Ipv6StartupSummary.describe(p);assertTrue(s.contains("tcp=4660->7000 flags=2 seq=42"));assertFalse(s.contains("PRIVATE"))}
 @Test fun ethernetOffsetIsRespected(){val p=ByteArray(58);p[14]=0x60;p[19]=4;p[20]=58;p[54]=135.toByte();assertTrue(Ipv6StartupSummary.describe(p,14).contains("icmp6=135"))}
 @Test fun rejectsShortAndInvalidOffsets(){for(n in 0..39)assertEquals("truncated_ipv6",Ipv6StartupSummary.describe(ByteArray(n)));assertEquals("truncated_ipv6",Ipv6StartupSummary.describe(ByteArray(40),-1));assertEquals("truncated_ipv6",Ipv6StartupSummary.describe(ByteArray(40),Int.MAX_VALUE));assertEquals("not_ipv6",Ipv6StartupSummary.describe(ByteArray(40)))}
 @Test fun paddingAndExtensionHeadersAreNotParsedAsTcp(){val p=ByteArray(80);p[0]=0x60;p[6]=6;assertFalse(Ipv6StartupSummary.describe(p).contains("tcp="));p[5]=40;p[6]=0;assertFalse(Ipv6StartupSummary.describe(p).contains("tcp="));p[5]=80;assertTrue(Ipv6StartupSummary.describe(p).contains("truncated=true"))}
}
