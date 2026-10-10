package local.headunit.diplay;
import android.content.*;import android.os.*;import java.io.Closeable;import java.util.concurrent.*;import org.json.JSONObject;
/** Connection-owned passive OEM receiver, bounded dispatch to the same phone session. */
public final class WheelControl extends BroadcastReceiver implements Closeable {
 public interface Sink {Object session();boolean send(Object session,int action);}
 private final Context context;private final Sink sink;private final WheelKeyPolicy policy=new WheelKeyPolicy();
 private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(4));
 private volatile boolean closed;private boolean registered;
 private static boolean supported(Context c){try{return Build.VERSION.SDK_INT==17&&"MX9A0".equals(Build.MODEL)&&c.getPackageManager().getPackageInfo("com.byd.multimediaservice",0).versionCode==382;}catch(Exception e){return false;}}
 public WheelControl(Context c,Sink s){this(c,s,supported(c));}
 WheelControl(Context c,Sink s,boolean supported){context=c.getApplicationContext();sink=s;try{if(supported){context.registerReceiver(this,new IntentFilter("APP_REV_DATA"));registered=true;log(0,"receiver_registered");}}catch(Exception e){log(0,"receiver_unavailable");}}
 public boolean duplicateAndroid(int key){return !closed&&registered&&!MainActivity.wheelProbeActive()&&sink.session()!=null&&policy.duplicateAndroid(key,SystemClock.elapsedRealtime());}
 public void onReceive(Context c,Intent i){if(closed||MainActivity.wheelProbeActive()||!"APP_REV_DATA".equals(i.getAction()))return;try{final int action=WheelKeyPolicy.decode(i.getByteArrayExtra("list"));if(action==0)return;final Object session=sink.session();if(session==null)return;final long received=SystemClock.elapsedRealtime();if(!policy.accept(action,received)){log(action,"duplicate_ignored");return;}try{worker.execute(new Runnable(){public void run(){if(closed)return;if(MainActivity.wheelProbeActive()||sink.session()!=session||SystemClock.elapsedRealtime()-received>600){log(action,"stale_ignored");return;}try{log(action,sink.send(session,action)?"reports_written_not_phone_acknowledged":"report_write_failed");}catch(Exception e){log(action,"send_failed_"+e.getClass().getSimpleName());}}});}catch(RejectedExecutionException e){log(action,"queue_full");}}catch(Exception e){log(0,"invalid_event");}}
 private void log(int action,String result){try{MainActivity.record(context,"WHEEL_CONTROL",new JSONObject().put("action",action).put("result",result).put("connection",WiredService.currentConnectionId()));}catch(Exception ignored){}}
 public void close(){if(closed)return;closed=true;if(registered){registered=false;try{context.unregisterReceiver(this);}catch(Exception ignored){}}worker.shutdownNow();}
}
