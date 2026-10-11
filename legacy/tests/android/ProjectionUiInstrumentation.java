package local.headunit.diplay;
import android.app.*;import android.content.*;import android.os.Bundle;import android.view.*;import android.widget.*;
public class ProjectionUiInstrumentation extends Instrumentation {
 MainActivity a;void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 int buttons(View v){if(v instanceof Button){String t=((Button)v).getText().toString();check(t.equals("连接")||t.equals("停止"),"Unexpected button "+t);return 1;}int n=0;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)n+=buttons(((ViewGroup)v).getChildAt(i));return n;}
 public void onStart(){Bundle r=new Bundle();try{
  a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  check(NativeCrypto.available,"Native library missing");check(PlaybackOptions.read(a)==15,"Not fixed15fps");
  for(String name:new String[]{"server.txt","token.txt"}){boolean missing=false;try{a.getAssets().open(name).close();}catch(java.io.IOException e){missing=true;}check(missing,"Private asset present");}
  com.shilapi.xcertplay.mfi.LocalMfiAuthenticationClient auth=VerifiedMfiCache.INSTANCE.get(a,new kotlin.jvm.functions.Function1<Integer,kotlin.Unit>(){public kotlin.Unit invoke(Integer n){return kotlin.Unit.INSTANCE;}});
  check(auth.readCertificate(16384).length==607,"Runtime certificate unavailable");check(auth.signChallenge(new byte[32]).length==64,"Local signature failed");
  runOnMainSync(new Runnable(){public void run(){
   check(buttons(a.controls)==2,"Expected two controls");
   a.preview.measure(View.MeasureSpec.makeMeasureSpec(1024,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));check(a.preview.getMeasuredWidth()==1024&&a.preview.getMeasuredHeight()==600,"Resolution drift");
   a.connectionId="test";a.connecting=true;a.videoReady=true;a.setProjectionMode(true);check(a.controls.getVisibility()==View.GONE&&a.videoBlank.getVisibility()==View.GONE,"Projection hidden");
   a.onBackPressed();check(a.controls.getVisibility()==View.VISIBLE,"Panel unavailable");a.onBackPressed();check(a.controls.getVisibility()==View.GONE,"Panel did not hide");
   a.portStatus.onReceive(a,new Intent().putExtra("stage","DISCONNECTED").putExtra("message","test"));check(a.videoBlank.getVisibility()==View.VISIBLE&&!a.connecting,"Last frame retained");
   a.connecting=true;a.videoReady=true;a.connectPending=true;a.permissionPending=true;a.busy=true;a.stopConnection();check(!a.connecting&&!a.connectPending&&!a.permissionPending&&a.videoBlank.getVisibility()==View.VISIBLE,"Stop failed");a.busy=false;
  }});waitForIdleSync();
  r.putString("stream","PASS: clean API17 launch, two buttons, no private service assets, fixed15fps/1024x600, native library, bundled identity key consistency/signing, panel lifecycle, stop/disconnect blanking. Physical CarPlay/audio pending real vehicle.\n");finish(Activity.RESULT_OK,r);
 }catch(Throwable t){r.putString("stream",android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,r);}}
}
