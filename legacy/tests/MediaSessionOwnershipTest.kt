package local.headunit.diplay
import com.shilapi.xcertplay.airplay.*
import java.io.Closeable
import java.net.Socket
import org.junit.Assert.*
import org.junit.Test

class MediaSessionOwnershipTest {
 @Suppress("UNCHECKED_CAST") private fun map(engine:CarPlayMediaEngine,name:String):MutableMap<Any,Any>{val f=engine.javaClass.getDeclaredField(name);f.isAccessible=true;return f.get(engine) as MutableMap<Any,Any>}
 private fun session(engine:CarPlayMediaEngine)=AirPlaySession(Socket(),AirPlayConfig("test","02:00:00:00:00:17","02:00:00:00:00:17","320.20",AirPlayDisplayConfig(800,480)),AirPlayIdentity(ByteArray(32),ByteArray(32),"test"),PairingStore(),null,object:AirPlaySessionListener{},engine)
 @Test fun closingSessionAfterTunnelListenCannotLeavePendingRelay(){
  lateinit var current:AirPlaySession;var closedDuringSetup=false;val engine=CarPlayMediaEngine(object:MediaSink{});engine.setIapTunnelHandler{true}
  current=AirPlaySession(Socket(),AirPlayConfig("test","02:00:00:00:00:17","02:00:00:00:00:17","320.20",AirPlayDisplayConfig(800,480)),AirPlayIdentity(ByteArray(32),ByteArray(32),"test"),PairingStore(),null,object:AirPlaySessionListener{override fun onDebugLog(message:String){if(message.startsWith("AirPlay iAP tunnel listening address=")){closedDuringSetup=true;current.close()}}},engine)
  val secret=PairVerify::class.java.getDeclaredField("sharedSecret");secret.isAccessible=true;secret.set(current.pairVerify,ByteArray(32))
  val result=engine.onDataStream(current,mapOf("clientTypeUUID" to "E9459FD0-BCAD-4C45-820F-1E72447EF2F2","seed" to 1L,"streamConnectionID" to 1L))
  assertTrue(closedDuringSetup);assertTrue(current.isClosed);assertNull(result);assertTrue(map(engine,"streams").isEmpty());assertTrue(map(engine,"pendingIapTunnels").isEmpty())
 }
 @Test fun oldSessionCleanupPreservesSuccessorMetadataAndStopsOnlyOldOutput(){
  val stopped=ArrayList<AudioStreamId>();val engine=CarPlayMediaEngine(object:MediaSink{override fun onAudioStopped(id:AudioStreamId){stopped.add(id)}})
  val old=session(engine);val next=session(engine);val a=CarPlayMediaEngine.StreamKey(old,100,"media");val b=CarPlayMediaEngine.StreamKey(next,100,"media")
  var oldClosed=0;var newClosed=0
  val streams=map(engine,"streams");streams[a]=Closeable{oldClosed++};streams[b]=Closeable{newClosed++}
  val metadata=map(engine,"audioMeta");metadata[a]=Any();metadata[b]=Any()
  engine.onSessionClosed(old)
  assertEquals(1,oldClosed);assertEquals(0,newClosed);assertFalse(streams.containsKey(a));assertTrue(streams.containsKey(b));assertFalse(metadata.containsKey(a));assertTrue(metadata.containsKey(b));assertEquals(listOf(AudioStreamId(100,"media",old)),stopped)
  engine.onSessionClosed(old);assertEquals(1,oldClosed);assertEquals(1,stopped.size)
  engine.onSessionClosed(next);assertEquals(1,newClosed)
 }
}
