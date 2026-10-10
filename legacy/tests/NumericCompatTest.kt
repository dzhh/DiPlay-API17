package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
class NumericCompatTest {
 @Test fun coversSignedBoundaries(){for(n in listOf(Long.MIN_VALUE,-1000000001,-1000000000,-999999999,-1,0,1,Long.MAX_VALUE)){assertEquals(java.lang.Long.toUnsignedString(n),NumericCompat.unsignedLong(n));assertEquals(Math.floorDiv(n,1000000000L),NumericCompat.floorSeconds(n));assertEquals(Math.floorMod(n,1000000000L),NumericCompat.floorNanos(n))}}
}
