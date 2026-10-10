package local.headunit.diplay;
import android.content.Context;
import android.hardware.usb.*;
import java.io.*;
import org.json.*;

/** Read-only failure evidence. Never opens USB, requests permission, or resets a port. */
final class UsbPresenceLog {
 static void capture(Context c,String reason){try{
  UsbManager manager=(UsbManager)c.getSystemService(Context.USB_SERVICE);JSONArray framework=new JSONArray(),kernel=new JSONArray();
  for(UsbDevice d:manager.getDeviceList().values()){if(framework.length()>=32)break;framework.put(new JSONObject().put("vid",d.getVendorId()).put("pid",d.getProductId()).put("interfaces",d.getInterfaceCount()).put("permission",manager.hasPermission(d)));}
  File[] nodes=new File("/sys/bus/usb/devices").listFiles();int visited=0;
  if(nodes!=null)for(File node:nodes){if(++visited>128||kernel.length()>=32)break;String vid=read(new File(node,"idVendor"));if(vid.matches("[a-fA-F0-9]{4}"))kernel.put(new JSONObject().put("vid",vid).put("pid",read(new File(node,"idProduct"))).put("configuration",read(new File(node,"bConfigurationValue"))).put("speedMbps",read(new File(node,"speed"))).put("usbVersion",read(new File(node,"version"))));}
  MainActivity.record(c,"USB_PRESENCE",new JSONObject().put("reason",reason).put("framework",framework).put("sysfsReadable",nodes!=null).put("kernel",kernel));
 }catch(Exception e){try{MainActivity.record(c,"USB_PRESENCE",new JSONObject().put("error",e.getClass().getSimpleName()));}catch(Exception ignored){}}}
 static String read(File f){try{FileInputStream in=new FileInputStream(f);try{byte[] b=new byte[32];int n=in.read(b);return n>0?new String(b,0,n,"US-ASCII").trim():"";}finally{in.close();}}catch(Exception e){return "unreadable";}}
}
