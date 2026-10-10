package local.headunit.diplay;
import android.content.*;
import org.json.JSONObject;
public class AutoBootReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){String action=i==null?"":i.getAction();if(!Intent.ACTION_BOOT_COMPLETED.equals(action)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))return;try{MainActivity.record(c,"AUTO_BOOT",new JSONObject().put("action",action).put("enabled",AutoUsbService.enabled(c)));AutoUsbService.start(c);}catch(Exception e){android.util.Log.e("DiPlayAuto","Cannot start USB watcher",e);}}
}
