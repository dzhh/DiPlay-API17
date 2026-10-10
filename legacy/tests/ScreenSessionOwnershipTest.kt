package local.headunit.diplay

import com.shilapi.xcertplay.airplay.*
import java.net.Socket
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test

class ScreenSessionOwnershipTest {
    @Suppress("UNCHECKED_CAST")
    private fun registry(engine: CarPlayMediaEngine, name: String): Map<Any, Any> {
        val field = engine.javaClass.getDeclaredField(name); field.isAccessible = true
        return field.get(engine) as Map<Any, Any>
    }
    private fun session(engine: CarPlayMediaEngine, debug: MutableList<String> = mutableListOf()): AirPlaySession {
        val s = AirPlaySession(Socket(), AirPlayConfig("test", "02:00:00:00:00:17", "02:00:00:00:00:17", "320.20", AirPlayDisplayConfig(800,480)),
            AirPlayIdentity(ByteArray(32), ByteArray(32), "test"), PairingStore(), null,
            object: AirPlaySessionListener { override fun onDebugLog(message: String) { debug.add(message) } }, engine)
        val secret = PairVerify::class.java.getDeclaredField("sharedSecret"); secret.isAccessible = true; secret.set(s.pairVerify, ByteArray(32))
        return s
    }
    private fun stream(engine: CarPlayMediaEngine, session: AirPlaySession): ScreenStream =
        registry(engine,"streams")[CarPlayMediaEngine.StreamKey(session,110)] as ScreenStream
    private fun listener(stream: ScreenStream): ScreenStream.Listener {
        val field = ScreenStream::class.java.getDeclaredField("listener"); field.isAccessible = true
        return field.get(stream) as ScreenStream.Listener
    }
    private fun join(stream: ScreenStream) {
        val field = ScreenStream::class.java.getDeclaredField("thread"); field.isAccessible = true
        val thread = field.get(stream) as? Thread ?: return
        thread.join(1500); assertFalse("Screen receiver leaked", thread.isAlive)
    }
    private val setup = mapOf<String,Any?>("streamConnectionID" to 1L)

    @Test fun oldSessionCallbacksAndTeardownCannotAffectSuccessor() {
        val frames = mutableListOf<Int>(); val active = mutableListOf<Boolean>()
        val diagnostics = mutableListOf<(String)->Unit>(); val recovery = mutableListOf<()->Unit>()
        val engine = CarPlayMediaEngine(object: MediaSink {
            override fun onVideoFrame(type:Int, naluBytes:ByteArray) { frames.add(naluBytes[0].toInt()) }
            override fun onScreenStreamActive(type:Int, state:Boolean) { active.add(state) }
            override fun setVideoDiagnosticHandler(type:Int, handler:(String)->Unit) { diagnostics.add(handler) }
            override fun setVideoRecoveryHandler(type:Int, handler:()->Unit) { recovery.add(handler) }
        })
        val oldDebug=mutableListOf<String>(); val newDebug=mutableListOf<String>()
        val old=session(engine,oldDebug); val next=session(engine,newDebug)
        assertNotNull(engine.onScreen(old,110,setup)); val a=stream(engine,old); val late=listener(a)
        assertNotNull(engine.onScreen(next,110,setup)); val b=stream(engine,next); val current=listener(b)
        try {
            late.onFrame(byteArrayOf(1)); current.onFrame(byteArrayOf(2))
            val oldMessages=oldDebug.size; diagnostics.first()("old marker"); diagnostics.last()("new marker")
            assertEquals(oldMessages,oldDebug.size); assertTrue(newDebug.contains("Video: new marker"))
            val at=System.nanoTime(); recovery.first()(); assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-at)<300)
            late.onClosed(null); engine.onTeardown(old,110); old.close()
            assertFalse(next.isClosed); assertEquals(listOf(true,true),active)
            current.onFrame(byteArrayOf(3)); assertEquals(listOf(2,3),frames)
            assertEquals(1,registry(engine,"screenOwners").size)
        } finally { old.close(); next.close(); join(a); join(b) }
        assertEquals(listOf(true,true,false),active)
    }

    @Test fun retiredSessionCannotRetakeScreenWithLateSetup() {
        val engine=CarPlayMediaEngine(object:MediaSink{}); val old=session(engine); val next=session(engine)
        engine.onScreen(old,110,setup); val a=stream(engine,old); engine.onScreen(next,110,setup); val b=stream(engine,next)
        try { assertNull(engine.onScreen(old,110,mapOf("streamConnectionID" to 2L))); assertSame(b,stream(engine,next)); assertEquals(1,registry(engine,"streams").size) }
        finally { old.close();next.close();join(a);join(b) }
    }

    @Test fun sameSessionCanReplaceItsOwnScreen() {
        val frames=mutableListOf<Int>();val engine=CarPlayMediaEngine(object:MediaSink{override fun onVideoFrame(type:Int,naluBytes:ByteArray){frames.add(naluBytes[0].toInt())}})
        val current=session(engine);engine.onScreen(current,110,setup);val a=stream(engine,current);val late=listener(a)
        assertNotNull(engine.onScreen(current,110,mapOf("streamConnectionID" to 2L)));val b=stream(engine,current)
        try{late.onFrame(byteArrayOf(1));listener(b).onFrame(byteArrayOf(2));assertEquals(listOf(2),frames)}finally{current.close();join(a);join(b)}
    }

    @Test fun replacementWaitsForInflightFrameWithoutLettingLateFrameCrossHandover() {
        val entered=CountDownLatch(1);val release=CountDownLatch(1);val replacement=CountDownLatch(1)
        val frames=java.util.Collections.synchronizedList(mutableListOf<Int>());val failure=AtomicReference<Throwable>()
        val engine=CarPlayMediaEngine(object:MediaSink {override fun onVideoFrame(type:Int,naluBytes:ByteArray){if(naluBytes[0].toInt()==1){entered.countDown();check(release.await(3,TimeUnit.SECONDS))};frames.add(naluBytes[0].toInt())}})
        val old=session(engine);val next=session(engine);engine.onScreen(old,110,setup);val a=stream(engine,old);val late=listener(a)
        val rx=Thread{try{late.onFrame(byteArrayOf(1))}catch(t:Throwable){failure.set(t)}};rx.start();assertTrue(entered.await(2,TimeUnit.SECONDS))
        val setupThread=Thread{try{assertNotNull(engine.onScreen(next,110,setup))}catch(t:Throwable){failure.set(t)}finally{replacement.countDown()}};setupThread.start()
        try { assertFalse(replacement.await(100,TimeUnit.MILLISECONDS)) } finally { release.countDown() }
        assertTrue(replacement.await(3,TimeUnit.SECONDS));rx.join(1500);setupThread.join(1500)
        val b=stream(engine,next)
        try { assertNull(failure.get());late.onFrame(byteArrayOf(3));listener(b).onFrame(byteArrayOf(2));assertEquals(listOf(1,2),frames) }
        finally { old.close();next.close();join(a);join(b) }
    }

    @Test fun handlerFailureCannotLeaveScreenRegistered() {
        val engine=CarPlayMediaEngine(object:MediaSink {
            override fun setVideoDiagnosticHandler(type:Int,handler:(String)->Unit){throw IllegalStateException("handler failed")}
            override fun onScreenStreamActive(type:Int,active:Boolean){if(!active)throw IllegalStateException("stop callback failed")}
        });val current=session(engine)
        try { engine.onScreen(current,110,setup);fail("Expected handler failure") }catch(expected:IllegalStateException){}
        assertTrue(registry(engine,"streams").isEmpty());assertTrue(registry(engine,"screenOwners").isEmpty());current.close()
    }
}
