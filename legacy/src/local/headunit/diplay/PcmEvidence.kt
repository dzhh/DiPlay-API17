package local.headunit.diplay

/** Aggregate amplitude only; no speech samples are retained or uploaded. */
class PcmEvidence {
 var peak=0;private set
 var nonzero=0L;private set
 fun observe(pcm:ByteArray){
  require(pcm.size%2==0)
  for(i in pcm.indices step 2){val value=((pcm[i].toInt() and 255) or (pcm[i+1].toInt() shl 8)).toShort().toInt();val amplitude=kotlin.math.abs(value);if(amplitude>peak)peak=amplitude;if(value!=0)nonzero++}
 }
 companion object {
  /** Duplicate little-endian mono samples without changing rate, duration or amplitude. */
  fun stereo(mono:ByteArray):ByteArray {
   require(mono.size%2==0&&mono.size<=1024*1024)
   val out=ByteArray(mono.size*2)
   var j=0
   for(i in mono.indices step 2){out[j++]=mono[i];out[j++]=mono[i+1];out[j++]=mono[i];out[j++]=mono[i+1]}
   return out
  }
 }
}
