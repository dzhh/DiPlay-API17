package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test

class AudioInputTest {
 @Test fun burstDropsOldestRatherThanDiscardingNewSpeech(){val q=AudioInput(3){0};for(n in 0..4)q.offer(ByteArray(12),n);assertEquals(3,q.size());assertEquals(2,q.drops());assertEquals(listOf(2,3,4),List(3){q.poll(0)!!.sample});assertNull(q.poll(0))}
 @Test fun oldQueuedAndHeldPacketsHaveSameLatencyBound(){var now=0L;val q=AudioInput(3){now};q.offer(ByteArray(12),1);val held=q.poll(0)!!;q.offer(ByteArray(12),2);now=251000000L;assertTrue(q.expired(held));assertNull(q.poll(0));assertEquals(1,q.drops());q.offer(ByteArray(12),3);assertEquals(3,q.poll(0)!!.sample)}
 @Test fun queuedDataOwnsItsBuffer(){val q=AudioInput(2){0};val bytes=ByteArray(12){1};q.offer(bytes,7);bytes[0]=9;assertEquals(1,q.poll(0)!!.bytes[0].toInt());q.offer(ByteArray(12),2);q.clear();assertNull(q.poll(0))}
 @Test fun malformedOrOversizedPacketsCannotGrowQueue(){val q=AudioInput(2){0};q.offer(ByteArray(11),0);q.offer(ByteArray(16385),0);assertEquals(2,q.drops());assertEquals(0,q.size())}
}
