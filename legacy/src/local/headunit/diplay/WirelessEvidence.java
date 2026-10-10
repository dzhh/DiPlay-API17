package local.headunit.diplay;

import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.content.pm.*;
import android.net.wifi.*;
import android.os.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

/** On-demand read-only wireless evidence. Never enable radios, pair, scan, send
 * vendor commands, open device nodes, or read credentials/contact payloads. */
final class WirelessEvidence {
 static JSONObject collect(final Context c)throws Exception {
  final JSONObject out=new JSONObject().put("schema",1).put("wirelessCarPlayProven",false);
  EnvironmentCheck.section(out,"standardBluetooth",new EnvironmentCheck.Check(){public JSONObject run()throws Exception{
   JSONObject j=new JSONObject().put("featureDeclared",c.getPackageManager().hasSystemFeature("android.hardware.bluetooth"));
   BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();j.put("adapterPresent",a!=null);
   if(a!=null)j.put("enabled",a.isEnabled()).put("state",a.getState()).put("scanMode",a.getScanMode()).put("bondedCount",a.getBondedDevices().size());
   return j.put("rfcomm","not_attempted_no_profile_connection_changes");
  }});
  EnvironmentCheck.section(out,"wifi",new EnvironmentCheck.Check(){public JSONObject run()throws Exception{
   WifiManager w=(WifiManager)c.getSystemService(Context.WIFI_SERVICE);JSONObject j=new JSONObject().put("managerPresent",w!=null);
   j.put("featureDeclared",c.getPackageManager().hasSystemFeature("android.hardware.wifi")).put("p2pFeatureDeclared",c.getPackageManager().hasSystemFeature("android.hardware.wifi.direct"));
   if(w==null)return j;
   j.put("enabled",w.isWifiEnabled()).put("state",w.getWifiState());WifiInfo info=w.getConnectionInfo();
   if(info!=null)j.put("supplicant",String.valueOf(info.getSupplicantState())).put("linkSpeedMbps",info.getLinkSpeed()).put("hasIpv4",info.getIpAddress()!=0);
   JSONArray methods=new JSONArray();for(String name:new String[]{"getWifiApState","getWifiApConfiguration","setWifiApEnabled","is5GHzBandSupported"}){
    boolean found=false;for(Method m:w.getClass().getMethods())if(m.getName().equals(name)){found=true;break;}if(found)methods.put(name);
   }j.put("availableMethodNames",methods);
   try{j.put("apState",w.getClass().getMethod("getWifiApState").invoke(w));}catch(Exception e){j.put("apStateStatus","unavailable_or_denied");}
   // Only classify cached scan frequencies; never emit SSID, BSSID or passwords.
   List<ScanResult> scans=w.getScanResults();int count=0,two=0,five=0;if(scans!=null)for(ScanResult scan:scans){if(count++>=128)break;if(scan.frequency>=2400&&scan.frequency<2500)two++;if(scan.frequency>=4900&&scan.frequency<5900)five++;}
   j.put("cachedScanCount",Math.min(count,128)).put("cached2GHzCount",two).put("cached5GHzCount",five).put("hotspotOperationProven",false).put("appleBeaconElements","not_measured");
   return j;
  }});
  EnvironmentCheck.section(out,"networkInterfaces",new EnvironmentCheck.Check(){public JSONObject run()throws Exception{
   JSONArray rows=new JSONArray();Enumeration<NetworkInterface> all=NetworkInterface.getNetworkInterfaces();
   if(all!=null)while(all.hasMoreElements()&&rows.length()<32){NetworkInterface n=all.nextElement();int v4=0,v6=0;Enumeration<InetAddress> addresses=n.getInetAddresses();while(addresses.hasMoreElements()){InetAddress a=addresses.nextElement();if(a instanceof Inet6Address)v6++;else v4++;}
    rows.put(new JSONObject().put("name",n.getName()).put("up",n.isUp()).put("multicast",n.supportsMulticast()).put("ipv4Count",v4).put("ipv6Count",v6).put("driver",driver(n.getName())));
   }return new JSONObject().put("interfaces",rows);
  }});
  EnvironmentCheck.section(out,"vendorApi",new EnvironmentCheck.Check(){public JSONObject run()throws Exception{return vendorApi(c);}});
  EnvironmentCheck.section(out,"vendorSerialBinding",new EnvironmentCheck.Check(){public JSONObject run()throws Exception{return bindExisting(c);}});
  EnvironmentCheck.section(out,"passiveSerialObservation",new EnvironmentCheck.Check(){public JSONObject run()throws Exception{return observe(c);}});
  EnvironmentCheck.section(out,"deviceNodes",new EnvironmentCheck.Check(){public JSONObject run()throws Exception{
   JSONObject j=new JSONObject();for(String path:new String[]{"/dev/btdevice","/sys/class/bluetooth","/sys/class/rfkill"}){File f=new File(path);j.put(path,new JSONObject().put("exists",f.exists()).put("readable",f.canRead()).put("writable",f.canWrite()));}return j;
  }});
  return out.put("limits",new JSONArray().put("Disabled standard adapter does not prove absent vendor Bluetooth").put("byte[] vendor command interface does not prove arbitrary RFCOMM or iAP2 support").put("No radio state, Wi-Fi credentials, routing or vendor profile changed").put("Missing broadcasts during a short sample are inconclusive").put("No phone names, MAC addresses, contact data or Bluetooth payload bytes collected"));
 }
 static JSONObject driver(String name)throws Exception {
  JSONObject j=new JSONObject();if(!name.matches("[A-Za-z0-9_.:-]{1,32}"))return j;
  File root=new File("/sys/class/net/"+name+"/device");File driver=new File(root,"driver");
  if(driver.exists())j.put("name",driver.getCanonicalFile().getName());
  File event=new File(root,"uevent");if(event.canRead()){
   BufferedReader reader=new BufferedReader(new FileReader(event));try{for(int i=0;i<32;i++){String line=reader.readLine();if(line==null)break;if(line.length()>256)continue;int at=line.indexOf('=');if(at<1)continue;String key=line.substring(0,at);if(key.equals("DRIVER")||key.equals("MODALIAS")||key.equals("PRODUCT")||key.equals("SDIO_ID"))j.put(key,line.substring(at+1));}}finally{reader.close();}
  }return j;
 }
 static JSONObject vendorApi(Context c)throws Exception {
  JSONArray packages=new JSONArray(),services=new JSONArray(),interfaces=new JSONArray();PackageManager pm=c.getPackageManager();
  for(String id:new String[]{"com.android.bluetooth","com.byd.btservice","com.byd.btserialservice"}){
   try{PackageInfo p=pm.getPackageInfo(id,PackageManager.GET_SERVICES);packages.put(new JSONObject().put("package",id).put("versionCode",p.versionCode).put("versionName",p.versionName));if(p.services!=null)for(ServiceInfo s:p.services)services.put(new JSONObject().put("package",id).put("name",s.name).put("enabled",s.enabled).put("exported",s.exported).put("permission",s.permission==null?"":s.permission));}
   catch(PackageManager.NameNotFoundException e){packages.put(new JSONObject().put("package",id).put("status","not_installed"));}
  }
  for(String name:new String[]{"com.byd.btservice.IBtServiceNew","com.byd.btservice.IBtSerialService"}){
   JSONObject row=new JSONObject().put("class",name);try{Context pkg=c.createPackageContext("com.byd.btservice",Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);Class<?> type=Class.forName(name,false,pkg.getClassLoader());JSONArray methods=new JSONArray();for(Method m:type.getDeclaredMethods()){if(methods.length()>=64)break;JSONArray args=new JSONArray();for(Class<?> arg:m.getParameterTypes())args.put(arg.getName());methods.put(new JSONObject().put("name",m.getName()).put("args",args).put("returns",m.getReturnType().getName()));}row.put("methods",methods).put("invoked",false);}catch(Exception e){row.put("status",e.getClass().getSimpleName());}catch(LinkageError e){row.put("status",e.getClass().getSimpleName());}interfaces.put(row);
  }
  return new JSONObject().put("packages",packages).put("services",services).put("interfaces",interfaces);
 }
 static JSONObject bindExisting(Context c)throws Exception {
  if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Worker required");
  final CountDownLatch reply=new CountDownLatch(1);final AtomicBoolean connected=new AtomicBoolean();
  ServiceConnection listener=new ServiceConnection(){public void onServiceConnected(ComponentName name,IBinder binder){connected.set(binder!=null&&binder.isBinderAlive());reply.countDown();}public void onServiceDisconnected(ComponentName name){connected.set(false);}};
  Intent i=new Intent("com.byd.btservice.IBtSerialService").setClassName("com.byd.btserialservice","com.byd.btserialservice.BtSerialService");boolean bound=false;
  try{bound=c.bindService(i,listener,0);if(bound)reply.await(1500,TimeUnit.MILLISECONDS);return new JSONObject().put("bindAccepted",bound).put("connected",connected.get()).put("autoCreate",false).put("commandsSent",0).put("scope","existing_service_only_no_binder_commands");}
  finally{if(bound)try{c.unbindService(listener);}catch(IllegalArgumentException ignored){}}
 }
 static JSONObject observe(Context c)throws Exception {
  if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Worker required");
  final int[] counts=new int[3];final long[] moduleVersion={-1};final Object lock=new Object();
  BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context context,Intent i){try{synchronized(lock){if("ReciveBTVersion".equals(i.getAction())){int v=i.getIntExtra("version",-1);if(v>=0)moduleVersion[0]=v;return;}byte[] data=i.getByteArrayExtra("list");if(data==null||data.length>65536)return;counts[0]=Math.min(100000,counts[0]+1);counts[1]=Math.min(10000000,counts[1]+data.length);counts[2]=Math.max(counts[2],data.length);}}catch(RuntimeException ignored){}}};
  IntentFilter filter=new IntentFilter("INTENT_SERIAL_BT_DATA");filter.addAction("ReciveBTVersion");boolean registered=false;
  try{c.registerReceiver(receiver,filter);registered=true;Thread.sleep(2000);synchronized(lock){return new JSONObject().put("windowMs",2000).put("broadcastCount",counts[0]).put("totalBytes",counts[1]).put("largestMessageBytes",counts[2]).put("reportedModuleVersion",moduleVersion[0]).put("payloadStored",false).put("senderAuthenticated",false);}}
  finally{if(registered)c.unregisterReceiver(receiver);}
 }
}
