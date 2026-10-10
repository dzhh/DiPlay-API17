package local.headunit.diplay
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Short jitter queue; stale speech/music must not play seconds after the phone sent it. */
class AudioInput(capacity:Int=12,private val now:()->Long=System::nanoTime) {
 data class Packet(val bytes:ByteArray,val sample:Int,val arrivedNs:Long)
 private val queue=ArrayBlockingQueue<Packet>(capacity)
 private val dropped=AtomicLong()
 @Synchronized fun offer(bytes:ByteArray,sample:Int){
  if(bytes.size !in 12..16384){dropped.incrementAndGet();return}
  val packet=Packet(bytes.copyOf(),sample,now())
  if(!queue.offer(packet)){if(queue.poll()!=null)dropped.incrementAndGet();if(!queue.offer(packet))dropped.incrementAndGet()}
 }
 fun expired(packet:Packet)=now()-packet.arrivedNs>TimeUnit.MILLISECONDS.toNanos(250)
 fun drop(){dropped.incrementAndGet()}
 fun drops()=dropped.get()
 fun size()=queue.size
 fun clear()=queue.clear()
 fun poll(timeoutMs:Long):Packet? {
  var packet=queue.poll(timeoutMs,TimeUnit.MILLISECONDS)
  while(packet!=null&&expired(packet)){drop();packet=queue.poll()}
  return packet
 }
}
