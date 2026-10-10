package local.headunit.diplay
import android.media.*
import android.view.Surface
import com.shilapi.xcertplay.airplay.*
import com.shilapi.xcertplay.media.MediaCodecSupport
import java.io.Closeable
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/** Bounded API17 media workers. No modern AudioAttributes or codec buffer APIs. */
class LegacyMedia(surface:Surface?,private val width:Int,private val height:Int,private val log:(String)->Unit):MediaSink,Closeable {
 private val siriCapture=SiriCapturePolicy()
 fun speechMode(params:Map<String,Any?>){if(siriCapture.update(params))log("microphone_speech_mode=${siriCapture.mode}")}
 private class VideoTarget(val surface:Surface?)
 @Volatile private var videoTarget=VideoTarget(surface)
 @Synchronized fun setSurface(surface:Surface?){if(closed.get())return;if(videoTarget.surface===surface)return;videoTarget=VideoTarget(surface);videoInput.dropped()}
 @Volatile var renderedVideoFrames=0L;private set
 @Volatile var receivedVideo=false;private set
 @Volatile var routing:AudioRouting?=null
 fun setRoleVolume(role:String,value:Float){for(a in audio.values)if(a.role==role)a.volume(value)}
 @Volatile var playbackFocus:PlaybackFocus?=null
 fun setOutputVolume(value:Float){for(a in audio.values)a.volume(value)}
 @Volatile var onVideoFailure:((Throwable)->Unit)?=null
 private val closed=AtomicBoolean(false)
 private val videoInput=VideoInput()
 private val videoFailed=AtomicBoolean(false)
 private var lastRecoveryNs=0L
 private val recoveryWork=ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,ArrayBlockingQueue<Runnable>(1))
 @Volatile private var diagnostic:((String)->Unit)?=null
 @Volatile private var recovery:(()->Unit)?=null
 private val audio=ConcurrentHashMap<AudioStreamId,AudioOutput>()
 private val microphones=ConcurrentHashMap<AudioStreamId,Microphone>()
 private val videoThread=MediaWorkers.start("api17-video"){videoLoop()}
 override fun setVideoDiagnosticHandler(type:Int,handler:(String)->Unit){diagnostic=handler}
 override fun setVideoRecoveryHandler(type:Int,handler:()->Unit){recovery=handler}
 override fun onVideoConfig(type:Int,codecData:ByteArray){if(!closed.get()&&!videoFailed.get()&&videoInput.configure(codecData))log("video_configuration_changed bytes=${codecData.size}")}
 override fun onVideoFrame(type:Int,naluBytes:ByteArray){if(naluBytes.isNotEmpty())receivedVideo=true;if(closed.get()||videoFailed.get()||videoTarget.surface?.isValid!=true)return;if(!videoInput.offer(naluBytes))requestRecovery("queue_overflow")}
 @Synchronized private fun requestRecovery(reason:String){val now=System.nanoTime();if(now-lastRecoveryNs<TimeUnit.SECONDS.toNanos(2))return;lastRecoveryNs=now;log("video_recovery reason=$reason");try{recoveryWork.execute{if(!closed.get())runCatching{recovery?.invoke()}}}catch(_:RejectedExecutionException){}}
 private fun videoLoop(){var codec:MediaCodec?=null;var configured:ByteArray?=null;var frames=0;var stage="waiting_config";var target=videoTarget;var surfaceFrames=0
  var pending:VideoInput.Frame?=null;var buffers:Array<ByteBuffer>?=null
  val info=MediaCodec.BufferInfo();var inFlight=0;var skipped=0;var queueMaxMs=0L;var decodeMaxMs=0L;var lastStats=System.nanoTime()
  fun dispose(){runCatching{codec?.stop()};runCatching{codec?.release()};codec=null;configured=null;buffers=null;pending=null;inFlight=0}
  fun drain(){val c=codec?:return;stage="drain";var latest=-1;var latestPts=0L
   // Feed every reference frame to the decoder, but display only the newest ready output.
   try{for(i in 0 until 32){val n=c.dequeueOutputBuffer(info,0)
    if(n>=0){inFlight=maxOf(0,inFlight-1);if(latest>=0){c.releaseOutputBuffer(latest,false);skipped++};latest=n;latestPts=info.presentationTimeUs}
    else if(n==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)log("video_output_format_changed")
    else if(n!=MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)break
   }}finally{if(latest>=0){val render=target===videoTarget&&target.surface?.isValid==true;c.releaseOutputBuffer(latest,render);if(render){frames++;renderedVideoFrames++;surfaceFrames++;if(surfaceFrames==1)log("video_surface_first_frame")};decodeMaxMs=maxOf(decodeMaxMs,(System.nanoTime()/1000-latestPts)/1000);if(render&&frames==1){diagnostic?.invoke("first frame rendered");log("video_rendered_frames=1")}}}
  }
  try{while(!closed.get()) {
   if(target!==videoTarget){dispose();target=videoTarget;surfaceFrames=0;videoInput.dropped();log("video_surface_changed attached=${target.surface!=null}");if(target.surface?.isValid==true)requestRecovery("surface_restored")}
   if(target.surface?.isValid!=true){dispose();Thread.sleep(50);continue}
   try {
   // Drain before waiting for a new frame. Sparse updates must not delay already decoded output by 100ms.
   drain()
   if(pending==null)pending=videoInput.poll(if(inFlight==0)100 else 5)
   val packet=pending
   if(packet!=null&&packet.config!==configured){stage="configure";require(target.surface?.isValid==true){"Video Surface is invalid"};runCatching{codec?.stop()};runCatching{codec?.release()};codec=null
    val(sps,pps)=MediaCodecSupport.avcParameterSets(packet.config);require(sps.isNotEmpty()&&pps.isNotEmpty()){"Invalid AVC parameter sets"}
    val format=MediaFormat.createVideoFormat("video/avc",width,height);format.setByteBuffer("csd-0",ByteBuffer.wrap(byteArrayOf(0,0,0,1)+sps));format.setByteBuffer("csd-1",ByteBuffer.wrap(byteArrayOf(0,0,0,1)+pps));format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,1024*1024)
    val c=MediaCodec.createDecoderByType("video/avc");codec=c;c.configure(format,target.surface,null,0);c.start();buffers=c.inputBuffers;inFlight=0;configured=packet.config;log("video_decoder_started name=default-avc width=$width height=$height")
   }
   val c=codec?:continue
   if(packet!=null){stage="input";val index=c.dequeueInputBuffer(5000)
    if(index>=0){val buffer=buffers!![index];buffer.clear();val byteCount=VideoBufferWriter.putAnnexB(packet.bytes,buffer);queueMaxMs=maxOf(queueMaxMs,(System.nanoTime()-packet.arrivedNs)/1000000);c.queueInputBuffer(index,0,byteCount,System.nanoTime()/1000,0);inFlight++;pending=null}
    // A brief busy decoder is normal. Retain this reference frame instead of destroying the GOP every 20ms.
    else if(System.nanoTime()-packet.arrivedNs>TimeUnit.MILLISECONDS.toNanos(250)){pending=null;videoInput.dropped();requestRecovery("decoder_backpressure")}
   }
   drain()
   val now=System.nanoTime();if(now-lastStats>=TimeUnit.SECONDS.toNanos(5)){log("video_progress rendered=$frames skippedOutput=$skipped queued=${videoInput.size()} queueMaxMs=$queueMaxMs decodeMaxMs=$decodeMaxMs");lastStats=now;queueMaxMs=0;decodeMaxMs=0}
   }catch(e:Throwable){if(e is InterruptedException)throw e;if(target!==videoTarget||target.surface?.isValid!=true){dispose();videoInput.dropped();log("video_surface_unavailable stage=$stage; session retained")}else throw e}
  }}catch(e:InterruptedException){}catch(e:Throwable){if(!closed.get()){videoFailed.set(true);videoInput.clear();log("video_failed stage=$stage type=${e.javaClass.simpleName} detail=${e.message} surfaceValid=${target.surface?.isValid} at=${e.stackTrace.take(4).joinToString()}");onVideoFailure?.invoke(e)}}finally{runCatching{codec?.stop()};runCatching{codec?.release()}}
 }
 @Synchronized override fun onAudioStarted(id:AudioStreamId,format:com.shilapi.xcertplay.airplay.AudioFormat,firstSample:Int){if(closed.get())return;log("audio_stream_start type=${id.type} kind=${id.audioType} codec=${format.codec}");val previous=audio.keys.filter{it.type==id.type&&it.audioType==id.audioType};for(key in previous)audio.remove(key)?.let{it.close();check(it.released()){"Previous audio worker is still releasing"}};audio[id]=AudioOutput(id,format)}
 override fun onAudioRtp(id:AudioStreamId,format:com.shilapi.xcertplay.airplay.AudioFormat,rtp:ByteArray,sample:Int){audio[id]?.offer(rtp,sample)}
 @Synchronized override fun onAudioStopped(id:AudioStreamId){audio.remove(id)?.let{log("audio_stopped type=${id.type} kind=${id.audioType}");it.close();val focusRole=if(it.role=="assistant")"media" else it.role;if(audio.values.none{v->(if(v.role=="assistant")"media" else v.role)==focusRole})routing?.playbackEnded(focusRole)}}
 @Synchronized override fun onMicrophoneStarted(id:AudioStreamId,config:MicrophoneConfig){if(closed.get())return;val previous=microphones.values.toList();for(m in previous)m.close();check(previous.all{it.released()}){"Previous microphone is still releasing"};microphones.clear();microphones[id]=Microphone(config,routing?.captureContext()?.let{AudioRouting.secondMusic(it)==10}?:false)}
 @Synchronized override fun onMicrophoneStopped(id:AudioStreamId){microphones.remove(id)?.close()}
 inner class AudioOutput(id:AudioStreamId,private val f:com.shilapi.xcertplay.airplay.AudioFormat):Closeable {
  val role=LegacyAudioPolicy.role(id.type,f.audioType);private val stream=routing?.stream(role)?:AudioManager.STREAM_MUSIC
  private var playing=false;private val navigationActivity=NavigationActivity()
  private val stop=AtomicBoolean(false);private val queue=AudioInput();@Volatile private var track:AudioTrack?=null
  private val outputChannels=if(role=="assistant"&&f.channels==1)2 else f.channels
  private val outputRate=if(role=="assistant"&&f.sampleRate==24000)48000 else f.sampleRate
  private val rateConverter=if(outputRate!=f.sampleRate)PcmDoubleRate(outputChannels)else null
  private val pcmEvidence=PcmEvidence();@Volatile private var lastHead=0L
  @Volatile private var appliedVolume=Float.NaN;private var bytesWritten=0L;private var mutedPackets=0L;private var lastProgress=0L;private var checkpoints=0
  private val thread=MediaWorkers.start("api17-audio"){loop()}
  fun offer(b:ByteArray,t:Int){if(!stop.get())queue.offer(b,t)}
  @Synchronized fun volume(value:Float){val t=track?:return;if(appliedVolume==value)return;runCatching{if(t.setStereoVolume(value,value)==AudioTrack.SUCCESS)appliedVolume=value}}
  private fun stopIdleNavigation(){if(playing&&stream==10&&!navigationActivity.active(System.nanoTime())){track?.stop();playing=false;log("navigation_idle_stopped")}}
  private fun write(input:ByteArray){val channelBytes=if(outputChannels!=f.channels)PcmEvidence.stereo(input)else input;val bytes=rateConverter?.convert(channelBytes)?:channelBytes;pcmEvidence.observe(bytes);if(stream==10&&!navigationActivity.packet(bytes,System.nanoTime())){stopIdleNavigation();return};val level=routing?.packet(role)?:playbackFocus?.packet()?:1f;volume(level);if(level==0f){mutedPackets++;progress();return};if(!playing){track!!.play();playing=true};var at=0;while(at<bytes.size&&!stop.get()){val n=track!!.write(bytes,at,bytes.size-at);if(n<=0)throw IllegalStateException("AudioTrack write failed");at+=n;bytesWritten+=n};if(bytesWritten==bytes.size.toLong())log("audio_first_pcm_written bytes=${bytes.size}");progress()}
  private fun progress(){val now=System.nanoTime();if(now-lastProgress<TimeUnit.SECONDS.toNanos(if(checkpoints<3||role=="assistant")1 else 10))return;lastHead=runCatching{track?.playbackHeadPosition?.toLong()?.and(0xffffffffL)?:lastHead}.getOrDefault(lastHead);lastProgress=now;val tag=if(checkpoints++<3)"audio_checkpoint" else "audio_progress";log("$tag role=$role stream=$stream written=$bytesWritten mutedPackets=$mutedPackets queued=${queue.size()} dropped=${queue.drops()} head=$lastHead peak=${pcmEvidence.peak} nonzero=${pcmEvidence.nonzero} volume=$appliedVolume")}
  private fun loop(){var decoder:MediaCodec?=null
   var inputBuffers:Array<ByteBuffer>?=null;var outputBuffers:Array<ByteBuffer>?=null;var inFlight=0
   val info=MediaCodec.BufferInfo()
   fun drain(){val c=decoder?:return
    for(i in 0 until 32){val n=c.dequeueOutputBuffer(info,0)
     if(n>=0){inFlight=maxOf(0,inFlight-1);val b=outputBuffers!![n];b.clear();require(info.offset>=0&&info.size>=0&&info.offset<=b.capacity()-info.size){"Invalid audio output range"};b.position(info.offset);b.limit(info.offset+info.size);val out=ByteArray(info.size);b.get(out);c.releaseOutputBuffer(n,false);if(out.isNotEmpty())write(out)}
     else if(n==MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)outputBuffers=c.outputBuffers
     else if(n!=MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)break
    }
   }
   try{require(f.channels in 1..2&&f.sampleRate in 8000..48000&&f.codec!=AudioCodecKind.OPUS)
    val channels=if(outputChannels==1)android.media.AudioFormat.CHANNEL_OUT_MONO else android.media.AudioFormat.CHANNEL_OUT_STEREO
    val min=AudioTrack.getMinBufferSize(outputRate,channels,android.media.AudioFormat.ENCODING_PCM_16BIT);require(min>0)
    val t=AudioTrack(stream,outputRate,channels,android.media.AudioFormat.ENCODING_PCM_16BIT,maxOf(min*2,outputRate*outputChannels/5),AudioTrack.MODE_STREAM)
    // Publish only after initial gain and its cache agree. Focus callbacks use the same lock.
    synchronized(this){track=t;require(t.state==AudioTrack.STATE_INITIALIZED);check(t.setStereoVolume(0f,0f)==AudioTrack.SUCCESS){"Initial audio gain rejected"};appliedVolume=0f}
    if(f.codec==AudioCodecKind.AAC_LC){val m=MediaFormat.createAudioFormat("audio/mp4a-latm",f.sampleRate,f.channels);m.setInteger("is-adts",1);val value=(2 shl 11) or (MediaCodecSupport.aacFrequencyIndex(f.sampleRate) shl 7) or (f.channels shl 3);m.setByteBuffer("csd-0",ByteBuffer.wrap(byteArrayOf((value shr 8).toByte(),value.toByte())));decoder=MediaCodec.createDecoderByType("audio/mp4a-latm");decoder.configure(m,null,null,0);decoder.start();inputBuffers=decoder.inputBuffers;outputBuffers=decoder.outputBuffers}
    log("audio_started role=$role stream=$stream codec=${f.codec} rate=${f.sampleRate} inputChannels=${f.channels} outputChannels=$outputChannels outputRate=$outputRate actualStream=${t.streamType} nativeRate=${AudioTrack.getNativeOutputSampleRate(stream)}");var packets=0;var pending:AudioInput.Packet?=null
    while(!stop.get()){
     drain()
     if(pending==null)pending=queue.poll(if(inFlight==0)100 else 5)
     val p=pending
     if(p==null){stopIdleNavigation();continue}
     if(queue.expired(p)){queue.drop();pending=null;continue}
     val c=decoder
     if(c==null){write(MicrophonePacketizer.toWirePcm(p.bytes.copyOfRange(12,p.bytes.size)));pending=null}
     else {val n=c.dequeueInputBuffer(5000)
      if(n>=0){val b=inputBuffers!![n];b.clear();val adts=MediaCodecSupport.adtsFrame(p.bytes.copyOfRange(12,p.bytes.size),f.sampleRate,f.channels);require(adts.size<=b.remaining());b.put(adts);c.queueInputBuffer(n,0,adts.size,(p.sample.toLong() and 0xffffffffL)*1000000/f.sampleRate,0);inFlight++;pending=null}
      // Preserve a packet across brief decoder backpressure; expiry bounds total latency.
     }
     if(pending==null&&++packets==1)log("audio_first_packet_written_or_queued")
     drain()
    }
   }catch(_:InterruptedException){}catch(e:Throwable){if(!stop.get())log("audio_failed="+e.javaClass.simpleName+" detail="+e.message+" written="+bytesWritten)}finally{log("audio_summary role=$role stream=$stream written=$bytesWritten mutedPackets=$mutedPackets dropped=${queue.drops()} headBeforeStop=$lastHead volume=$appliedVolume peak=${pcmEvidence.peak} nonzero=${pcmEvidence.nonzero}");stop.set(true);queue.clear();runCatching{decoder?.stop()};runCatching{decoder?.release()};runCatching{track?.stop()};runCatching{track?.release()};track=null}
  }
  fun released()=!thread.isAlive
  override fun close(){lastHead=runCatching{track?.playbackHeadPosition?.toLong()?.and(0xffffffffL)?:lastHead}.getOrDefault(lastHead);stop.set(true);queue.clear();runCatching{track?.pause()};runCatching{track?.flush()};thread.interrupt();if(!MediaWorkers.await(thread))log("audio_release_pending")}
 }
 inner class Microphone(private val c:MicrophoneConfig,private val supported:Boolean):Closeable {
  private val stop=AtomicBoolean(false);@Volatile private var recorder:AudioRecord?=null;@Volatile private var socket:DatagramSocket?=null
  private val thread=MediaWorkers.start("api17-mic"){capture()}
  private fun releaseRecorder(){val r=recorder?:return;runCatching{r.stop()};r.release();check(r.state==AudioRecord.STATE_UNINITIALIZED){"Microphone release incomplete"};recorder=null}
  private fun capture(){var captured=0L;var silence=0L
   try{
    require(c.codec==AudioCodecKind.LPCM&&c.channels==1&&c.sampleRate in 8000..48000&&c.frameBytes in 2..8192&&c.frameMillis in 1..100)
    val context=routing?.captureContext()
    val s=DatagramSocket();socket=s;val counters=MicrophoneCounters();val buffer=ByteArray(c.frameBytes);var filled=0;var paused=false;var nextSilenceNs=0L
    while(!stop.get()){
     if(!siriCapture.capture(supported,c.audioType)){
      if(!paused){releaseRecorder();filled=0;buffer.fill(0);paused=true;nextSilenceNs=System.nanoTime();log("microphone_playback_hold mode=${siriCapture.mode} hardwareReleased=true kind=${c.audioType}")}
      // No AudioRecord exists here. Silence keeps RTP sequence/timestamps continuous.
      val period=c.frameMillis*1000000L;nextSilenceNs+=period;val waitNs=nextSilenceNs-System.nanoTime()
      if(waitNs>0)Thread.sleep(waitNs/1000000L,(waitNs%1000000L).toInt())else if(waitNs< -period)nextSilenceNs=System.nanoTime()
      if(stop.get())break
      if(siriCapture.capture(supported,c.audioType))continue
      val p=MicrophonePacketizer.sealPacket(c.key,c.payloadType,counters,MicrophonePacketizer.toWirePcm(buffer),c.rtpSamplesPerPacket)
      s.send(DatagramPacket(p,p.size,c.host,c.port));silence++;continue
     }
     paused=false
     if(recorder==null){
      val min=AudioRecord.getMinBufferSize(c.sampleRate,android.media.AudioFormat.CHANNEL_IN_MONO,android.media.AudioFormat.ENCODING_PCM_16BIT);require(min>0){"Unsupported microphone buffer rate=${c.sampleRate} result=$min"}
      val r=AudioRecord(MediaRecorder.AudioSource.MIC,c.sampleRate,android.media.AudioFormat.CHANNEL_IN_MONO,android.media.AudioFormat.ENCODING_PCM_16BIT,maxOf(min*2,c.frameBytes*4));recorder=r;require(r.state==AudioRecord.STATE_INITIALIZED)
      if(stop.get())break
      val path=CarMicrophone.start(r,if(c.audioType.equals("speechrecognition",true))context else null)
      check(r.recordingState==AudioRecord.RECORDSTATE_RECORDING){"Microphone did not start"}
      log("microphone_capture_path=$path kind=${c.audioType} recordingState=${r.recordingState}")
      log("microphone_started_pcm kind=${c.audioType} rate=${c.sampleRate}")
      if(!siriCapture.capture(supported,c.audioType))continue
     }
     val n=recorder!!.read(buffer,filled,buffer.size-filled);if(n<=0)throw IOException("Microphone read failed");filled+=n
     if(!siriCapture.capture(supported,c.audioType)){filled=0;continue}
     if(filled==buffer.size){val p=MicrophonePacketizer.sealPacket(c.key,c.payloadType,counters,MicrophonePacketizer.toWirePcm(buffer),c.rtpSamplesPerPacket);s.send(DatagramPacket(p,p.size,c.host,c.port));captured++;filled=0}
    }
   }catch(_:InterruptedException){}catch(e:Throwable){if(!stop.get())log("microphone_failed="+e.javaClass.simpleName+" detail="+e.message)}finally{stop.set(true);runCatching{releaseRecorder()}.onFailure{log("microphone_release_failed="+it.javaClass.simpleName)};socket?.close();log("microphone_stopped kind=${c.audioType} capturedPackets=$captured silentPackets=$silence")}
  }
  fun released()=!thread.isAlive
  override fun close(){stop.set(true);socket?.close();runCatching{recorder?.stop()};thread.interrupt();if(!MediaWorkers.await(thread))log("microphone_release_pending")}
 }
 override fun close(){if(!closed.compareAndSet(false,true))return
  val outputs:List<AudioOutput>;val inputs:List<Microphone>
  synchronized(this){outputs=audio.values.toList();audio.clear();inputs=microphones.values.toList();microphones.clear()}
  recoveryWork.shutdownNow();videoInput.clear();videoThread.interrupt()
  for(a in outputs)a.close();for(m in inputs)m.close()
  if(!MediaWorkers.await(videoThread))log("video_release_pending");playbackFocus?.close();routing?.close()
 }
}
