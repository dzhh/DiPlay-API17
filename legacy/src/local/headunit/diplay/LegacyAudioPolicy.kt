package local.headunit.diplay

/** CarPlay roles remain distinct even when an OEM exposes only one Android output. */
object LegacyAudioPolicy {
 @JvmStatic fun role(type:Int,kind:String):String=when(kind.lowercase(java.util.Locale.US)) {
  "telephony" -> "call"
  "speechrecognition" -> "assistant"
  "media" -> "media"
  "default", "alert", "compatibility" -> if(type==102) "media" else "navigation"
  else -> if(type==102) "media" else "navigation"
 }
 @JvmStatic fun stream(role:String,secondMusic:Int):Int=if(role=="navigation"&&secondMusic==10)10 else 3
}
