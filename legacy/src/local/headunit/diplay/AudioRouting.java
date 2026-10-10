package local.headunit.diplay;
import android.content.*;import android.media.*;import android.os.*;import java.io.Closeable;import java.util.*;import org.json.*;

/** Firmware-audited navigation stream. Does not impersonate a phone hang-up or change car sources. */
public final class AudioRouting implements Closeable {
 private final Context context;private final LegacyMedia media;private final int secondMusic;
 private final Map<String,PlaybackFocus> focus=new HashMap<String,PlaybackFocus>();private boolean closed;
 public AudioRouting(Context c,LegacyMedia m){context=c.getApplicationContext();media=m;secondMusic=secondMusic(c);for(final String role:new String[]{"media","navigation","assistant","call"}){
  if(role.equals("assistant")||(role.equals("navigation")&&secondMusic==10))continue;
  focus.put(role,new PlaybackFocus(c,new PlaybackFocus.Volume(){public void set(float value){media.setRoleVolume(role,value);if(role.equals("media"))media.setRoleVolume("assistant",value);}},role.equals("media")?AudioManager.AUDIOFOCUS_GAIN:role.equals("navigation")?AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,role));
 }try{MainActivity.record(context,"AUDIO_ROUTING",new JSONObject().put("navigationStream",stream("navigation")).put("nativeNavigation",secondMusic==10).put("assistantStream",stream("assistant")).put("callStream",stream("call")).put("callHardwareRoute","requires_real_car_confirmation"));}catch(Exception ignored){}}
 public Context captureContext(){return context;}
 public int stream(String role){return LegacyAudioPolicy.stream(role,secondMusic);}
 public float packet(String role){PlaybackFocus f; synchronized(this){if(closed)return 0;f=focus.get(role.equals("assistant")?"media":role);}return f==null?1:f.packet();}
 public void playbackEnded(String role){PlaybackFocus f;synchronized(this){if(closed)return;f=focus.get(role.equals("assistant")?"media":role);}if(f!=null)f.playbackEnded();}
 public synchronized void foreground(){if(!closed)for(PlaybackFocus f:focus.values())f.foreground();}
 public synchronized void close(){if(closed)return;closed=true;for(PlaybackFocus f:focus.values())f.close();}
 public static int secondMusic(Context c){try{if(Build.VERSION.SDK_INT!=17||!"MX9A0".equals(Build.MODEL)||c.getPackageManager().getPackageInfo("com.byd.multimediaservice",0).versionCode!=382)return -1;int value=AudioManager.class.getField("STREAM_MUSIC_SECOND").getInt(null);return value==10?value:-1;}catch(Exception e){return -1;}}
 public static JSONObject evidence(Context c)throws Exception{AudioManager a=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);JSONArray streams=new JSONArray();for(java.lang.reflect.Field f:AudioManager.class.getFields()){if(!f.getName().startsWith("STREAM_")||f.getType()!=Integer.TYPE)continue;JSONObject item=new JSONObject().put("name",f.getName());try{int stream=f.getInt(null);item.put("id",stream).put("volume",a.getStreamVolume(stream)).put("max",a.getStreamMaxVolume(stream));}catch(Exception e){item.put("error",e.getClass().getSimpleName());}streams.put(item);}return new JSONObject().put("streams",streams).put("auditedNavigationStream",secondMusic(c)).put("callHardwareRoute","not_confirmed");}
}
