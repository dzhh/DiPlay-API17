package local.headunit.diplay

/** Replay only small IPv6 neighbor discovery packets, whose duplicates are harmless.
 * Never replay TCP, media, multi-packet NTBs or a reported partial USB transfer. */
object StartupUsbWrite {
 /** One startup-only discovery packet may bridge the kernel's multi-second RS gaps.
  * Yield on inbound data so its queued neighbor response is not held behind this packet.
  * Every ioctl is short; close/disconnect/partial success always stops replay. */
 fun awaitReady(transfer:(Int)->Int,check:()->Unit,keepWaiting:()->Boolean,
                nowMillis:()->Long,pause:()->Unit):Int {
  val started=nowMillis()
  var result=-110
  repeat(128){
   check()
   val remaining=15000L-(nowMillis()-started)
   if(remaining<=0)return result
   if(it>0&&!keepWaiting())return result
   result=transfer(minOf(150L,remaining).toInt())
   if(result!=-110&&result!=-11)return result
   if(!keepWaiting()||nowMillis()-started>=15000)return result
   pause()
  }
  return result
 }
 fun retryable(frame:ByteArray,blockSize:Int):Boolean {
  if(blockSize>512||frame.size<58)return false
  if(frame[12].toInt() and 255!=0x86||frame[13].toInt() and 255!=0xdd)return false
  if(frame[14].toInt() ushr 4 and 15!=6||frame[20].toInt() and 255!=58)return false
  val required=when(frame[54].toInt() and 255){133->8;135,136->24;else->return false}
  val declared=((frame[18].toInt() and 255) shl 8) or (frame[19].toInt() and 255)
  return declared>=required&&declared<=frame.size-54&&frame[55].toInt()==0
 }
 fun write(retry:Boolean,timeout:Int,transfer:(Int)->Int,check:()->Unit,pause:()->Unit):Int {
  val attempts=if(retry)minOf(8,maxOf(1,timeout/175)) else 1
  var result=0
  repeat(attempts){n->check();result=transfer(if(retry)minOf(timeout,150) else timeout)
   if(result!=-110&&result!=-11)return result
   if(n+1<attempts)pause()
  }
  return result
 }
}
