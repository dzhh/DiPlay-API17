package local.headunit.diplay;

import android.app.AlertDialog;
import android.content.*;
import android.media.AudioManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.UUID;

/** Guided observation only: never registers as media-button owner or consumes input. */
final class WheelProbe {
 static final String[] KEYS={"上一曲","下一曲","音量 +","音量 −","拨号 / 接听","挂断"};
 final MainActivity owner;final Handler handler=new Handler();final JSONObject report=new JSONObject();final JSONArray steps=new JSONArray();
 final long started=SystemClock.elapsedRealtime();AlertDialog dialog;TextView prompt,feedback;Button next;
 JSONObject step;JSONArray events;JSONObject headers;int index=-1,eventCount,omitted;boolean ended,registered,baselineDone;
 AudioManager audio;int secondStream;int music=-1,navigation=-1;long stepStart;int observed;String latest="等待按键";
 WheelProbe(MainActivity activity){owner=activity;}
 void start(){try{audio=(AudioManager)owner.getSystemService(Context.AUDIO_SERVICE);secondStream=AudioRouting.secondMusic(owner);
  report.put("schema",1).put("checkId",UUID.randomUUID().toString()).put("time",System.currentTimeMillis()).put("passive",true).put("kind","wheel_key_probe").put("appVersion",owner.getPackageManager().getPackageInfo(owner.getPackageName(),0).versionName).put("connection",WiredService.currentConnectionId()).put("carplayReady",WiredService.isVideoReady()).put("steps",steps).put("limits","32 key/volume/lifecycle observations and 16 header buckets per step; no phone numbers or contact payloads; unreceived events remain unknown");
  LinearLayout layout=new LinearLayout(owner);layout.setOrientation(1);layout.setPadding(24,12,24,12);
  prompt=new TextView(owner);prompt.setTextSize(22);layout.addView(prompt);feedback=new TextView(owner);feedback.setTextSize(15);feedback.setPadding(0,16,0,16);layout.addView(feedback);
  dialog=new AlertDialog.Builder(owner).setTitle("方向盘按键检测").setView(layout).setPositiveButton("下一项",null).setNegativeButton("结束并提交",null).create();dialog.setCanceledOnTouchOutside(false);
  dialog.setOnKeyListener(new DialogInterface.OnKeyListener(){public boolean onKey(DialogInterface d,int code,KeyEvent event){key(event,"dialog");return false;}});
  dialog.setOnCancelListener(new DialogInterface.OnCancelListener(){public void onCancel(DialogInterface d){finish("cancelled",true);}});
  dialog.show();next=dialog.getButton(AlertDialog.BUTTON_POSITIVE);next.setEnabled(false);next.setOnClickListener(new View.OnClickListener(){public void onClick(View v){advance();}});
  dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener(){public void onClick(View v){finish("partial",true);}});
  begin("baseline");IntentFilter filter=new IntentFilter("APP_REV_DATA");filter.addAction("APP_REV_BT_DATA");filter.addAction(Intent.ACTION_MEDIA_BUTTON);filter.addAction("android.media.VOLUME_CHANGED_ACTION");owner.registerReceiver(receiver,filter);registered=true;
  handler.post(poll);handler.postDelayed(new Runnable(){public void run(){if(ended)return;baselineDone=true;prompt.setText("基线已记录\n点击下一项开始逐个测试");next.setEnabled(true);}},3000);
  prompt.setText("请先不要按键，采集 3 秒背景数据\n请停车测试。拨号键可能触发原车回拨。");
 }catch(Exception e){finish("setup_failed",false);owner.showOperation("按键检测无法开始："+e.getClass().getSimpleName());}}
 void begin(String label)throws JSONException {step=new JSONObject().put("label",label).put("startElapsedMs",SystemClock.elapsedRealtime()-started);events=new JSONArray();headers=new JSONObject();step.put("events",events).put("broadcastHeaders",headers);steps.put(step);eventCount=0;omitted=0;observed=0;stepStart=SystemClock.elapsedRealtime();latest="等待按键";music=-1;navigation=-1;try{MainActivity.record(owner,"WHEEL_PROBE_STEP",new JSONObject().put("checkId",report.getString("checkId")).put("label",label).put("elapsedMs",stepStart-started));}catch(Exception ignored){}}
 void seal(String outcome)throws JSONException {if(step==null)return;step.put("endElapsedMs",SystemClock.elapsedRealtime()-started).put("outcome",outcome).put("omittedEvents",omitted).put("observations",observed);}
 void advance(){if(ended||!baselineDone)return;try{step.put("userConfirmedPress",index>=0);seal(index<0?"baseline":"user_confirmed_pressed");if(index==KEYS.length-1){finish("complete",true);return;}index++;begin(KEYS[index]);prompt.setText((index+1)+" / "+KEYS.length+"：请短按一次「"+KEYS[index]+"」\n按完并松开后点击下一项。\n若原车界面弹出，返回本检测后继续。");next.setText(index==KEYS.length-1?"完成并提交":"已按过，下一项");refresh();}catch(Exception e){finish("error",true);}}
 void event(JSONObject e){if(ended||step==null)return;try{observed++;if(eventCount<32){e.put("elapsedMs",SystemClock.elapsedRealtime()-started).put("stepMs",SystemClock.elapsedRealtime()-stepStart);events.put(e);eventCount++;try{MainActivity.record(owner,"WHEEL_PROBE_OBSERVATION",new JSONObject(e.toString()).put("checkId",report.getString("checkId")).put("label",step.getString("label")));}catch(Exception ignored){}}else omitted++;latest=e.optString("source");refresh();}catch(Exception ignored){}}
 void key(KeyEvent k,String route){if(k==null||ended)return;try{event(new JSONObject().put("source","android_key").put("route",route).put("action",k.getAction()).put("keyCode",k.getKeyCode()).put("scanCode",k.getScanCode()).put("deviceId",k.getDeviceId()).put("inputSource",k.getSource()).put("repeat",k.getRepeatCount()).put("flags",k.getFlags()).put("downTime",k.getDownTime()).put("eventTime",k.getEventTime()));}catch(Exception ignored){}}
 void lifecycle(String value){if(ended)return;try{event(new JSONObject().put("source","lifecycle").put("state",value));}catch(Exception ignored){}}
 void refresh(){if(feedback!=null&&!ended)feedback.setText("本项观察记录："+observed+"（含音量与界面变化）\n最近来源："+latest+"\n有记录不等于已识别出键值；没有记录也会保留结果。");}
 final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(ended)return;try{
  String action=i.getAction();if(Intent.ACTION_MEDIA_BUTTON.equals(action)){key((KeyEvent)i.getParcelableExtra(Intent.EXTRA_KEY_EVENT),"media_broadcast");return;}
  if("android.media.VOLUME_CHANGED_ACTION".equals(action)){volumes();return;}
  byte[] bytes=i.getByteArrayExtra("list");int group=bytes!=null&&bytes.length>0?bytes[0]&255:-1,command=bytes!=null&&bytes.length>1?bytes[1]&255:-1;
  String bucket=action+":"+group+":"+command+":"+(bytes==null?-1:bytes.length);if(!headers.has(bucket)&&headers.length()>=16)bucket="other_headers";headers.put(bucket,headers.optInt(bucket)+1);
  // Only the firmware's known key envelope is retained. BT contacts/call data never are.
  if("APP_REV_DATA".equals(action)&&group==4&&command==1){JSONArray data=new JSONArray();for(int n=0;n<Math.min(bytes.length,16);n++)data.put(bytes[n]&255);event(new JSONObject().put("source","oem_key_broadcast").put("action",action).put("length",bytes.length).put("bytes",data));}
 }catch(Exception e){try{event(new JSONObject().put("source","receiver_error").put("error",e.getClass().getSimpleName()));}catch(Exception ignored){}}}};
 void volumes(){try{AudioManager a=audio;int m=a.getStreamVolume(AudioManager.STREAM_MUSIC),n=secondStream==10?a.getStreamVolume(10):-1;if(m!=music||n!=navigation){boolean initial=music<0;music=m;navigation=n;event(new JSONObject().put("source",initial?"volume_snapshot":"volume_changed").put("music",m).put("navigation",n).put("mode",a.getMode()));}}catch(Exception ignored){}}
 final Runnable poll=new Runnable(){public void run(){if(ended)return;volumes();if(SystemClock.elapsedRealtime()-started>=10*60*1000){finish("timeout",true);return;}handler.postDelayed(this,300);}};
 void finish(String reason,boolean upload){if(ended)return;try{seal(reason);report.put("outcome",reason).put("durationMs",SystemClock.elapsedRealtime()-started);}catch(Exception ignored){}ended=true;handler.removeCallbacksAndMessages(null);if(registered){registered=false;try{owner.unregisterReceiver(receiver);}catch(Exception ignored){}}if(dialog!=null&&dialog.isShowing())dialog.dismiss();owner.wheelProbe=null;
  if(!owner.closed){owner.task("wheel",new MainActivity.Work(){public String run()throws Exception{EnvironmentCheck.record(owner,report);return upload?"按键检测结果已保存。\n"+owner.uploadNow():"按键检测已保留，可点击提交日志。";}});}else try{EnvironmentCheck.record(owner,report);}catch(Exception ignored){}
 }
}
