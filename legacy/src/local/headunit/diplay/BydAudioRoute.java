package local.headunit.diplay;
import android.content.*;import android.content.pm.*;import android.os.*;
import java.io.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;import org.json.JSONObject;

/** Audited MX9A0 Bluetooth service v182: music group 3, source control 6, off.
 * This releases source 19. It never sends AVRCP pause/stop, disables Bluetooth, or touches calls.
 */
public final class BydAudioRoute implements Closeable,ServiceConnection {
 public static final String DESCRIPTOR="com.byd.btservice.IBtService",PKG="com.byd.btservice";
 private static final ThreadPoolExecutor commands=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(1));
 private final Context context;private final Handler main=new Handler(Looper.getMainLooper());
 private final AtomicBoolean closed=new AtomicBoolean();private boolean bound,inFlight;private IBinder remote;private int attempts;
 public BydAudioRoute(Context c){context=c.getApplicationContext();}
 public void start(){main.post(new Runnable(){public void run(){if(closed.get())return;try{
  PackageInfo info=context.getPackageManager().getPackageInfo(PKG,0);
  if(!BydAudioPolicy.supported(Build.VERSION.SDK_INT,Build.MODEL,info.versionCode)){log("unsupported_version="+info.versionCode);close();return;}
  Intent intent=new Intent(DESCRIPTOR).setComponent(new ComponentName(PKG,"com.byd.btservice.BtService"));
  bound=context.bindService(intent,BydAudioRoute.this,Context.BIND_AUTO_CREATE);log("bind="+bound);
  if(!bound){close();return;}main.postDelayed(new Runnable(){public void run(){log("window_complete attempts="+attempts);close();}},8000);
 }catch(Exception e){log("unavailable="+e.getClass().getSimpleName());close();}}});}
 public void onServiceConnected(ComponentName name,IBinder binder){if(closed.get())return;remote=binder;send();main.postDelayed(new Runnable(){public void run(){send();}},1500);main.postDelayed(new Runnable(){public void run(){send();}},5000);}
 public void onServiceDisconnected(ComponentName name){log("disconnected");remote=null;close();}
 private void send(){if(closed.get()||remote==null||inFlight||attempts>=3)return;final IBinder target=remote;inFlight=true;attempts++;
  try{commands.execute(new Runnable(){public void run(){if(closed.get())return;String result;try{releaseSource(target);result="source_off_request_accepted";}catch(Exception e){result="source_off_failed="+e.getClass().getSimpleName();}final String text=result;main.post(new Runnable(){public void run(){inFlight=false;if(!closed.get())log(text+" attempt="+attempts);}});}});}catch(RejectedExecutionException e){inFlight=false;log("command_busy");}
 }
 public static void releaseSource(IBinder target)throws Exception{
  if(!DESCRIPTOR.equals(target.getInterfaceDescriptor()))throw new IOException("Unexpected BYD interface");
  Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(DESCRIPTOR);data.writeByte((byte)3);data.writeByte((byte)6);data.writeString("off");if(!target.transact(1,data,reply,0))throw new IOException("Source control rejected");reply.readException();}finally{data.recycle();reply.recycle();}
 }
 private void log(String message){try{MainActivity.record(context,"BYD_AUDIO_ROUTE",new JSONObject().put("detail",message).put("speakerConfirmed",false));}catch(Exception ignored){}}
 public void close(){if(!closed.compareAndSet(false,true))return;main.post(new Runnable(){public void run(){main.removeCallbacksAndMessages(null);remote=null;if(bound){bound=false;try{context.unbindService(BydAudioRoute.this);}catch(Exception ignored){}}}});}
}
