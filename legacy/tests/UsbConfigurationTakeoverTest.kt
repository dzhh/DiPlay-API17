package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class UsbConfigurationTakeoverTest {
 private class Fake:UsbConfigurationOps {
  var active=4;var busy=true;var failSwitch=false;var failDetach=false;var failRetry=false;var denyInterface=-1;var failRestore=false
  val drivers=linkedMapOf(0 to UsbDriver(error=-61),1 to UsbDriver(error=-61),2 to UsbDriver("ipheth"))
  val events=ArrayList<String>()
  override fun current()=active
  override fun configure(id:Int):Int{events.add("config:$id");if(failSwitch||id==4&&failRestore)return -5;if(id==6&&busy){busy=false;return -16};if(id==6&&failRetry)return -16;active=id;return 0}
  override fun interfaces(config:Int)=listOf(0,1,2)
  override fun driver(id:Int)=drivers.getValue(id)
  override fun bind(id:Int,connect:Boolean):Int{events.add("bind:$id:$connect");if((failDetach||id==denyInterface)&&!connect)return -1;drivers[id]=if(connect)UsbDriver("ipheth")else UsbDriver(error=-61);return 0}
  override fun log(message:String){events.add("log:$message")}
 }
 @Test fun invalidTargetCannotSendUsbCommands(){for(target in listOf(-1,0,256)){val f=Fake();try{UsbConfigurationTakeover(f,target);fail("Accepted invalid target")}catch(_:IllegalArgumentException){};assertTrue(f.events.isEmpty())}}
 @Test fun skipsResetIfAlreadyActive(){val f=Fake();f.active=6;f.drivers[1]=UsbDriver("usbfs");val s=UsbConfigurationTakeover(f,6);s.prepare();s.close();assertFalse(f.events.any{it.startsWith("config:")||it.startsWith("bind:")})}
 @Test fun activeConfigurationRestoresKernelDetachedDuringClaims(){val f=Fake();f.active=6;val s=UsbConfigurationTakeover(f,6);s.prepare();f.drivers[2]=UsbDriver(error=-61);s.close();assertTrue(f.events.contains("bind:2:true"));assertEquals("ipheth",f.drivers.getValue(2).name);assertFalse(f.events.any{it.startsWith("config:")})}
 @Test fun activeConfigurationNeverReplacesNewInterfaceOwner(){for(owner in listOf(UsbDriver("usbfs"),UsbDriver("other_kernel"),UsbDriver(error=-13))){val f=Fake();f.active=6;val s=UsbConfigurationTakeover(f,6);s.prepare();f.drivers[2]=owner;s.close();assertFalse(f.events.any{it.startsWith("bind:")||it.startsWith("config:")});assertEquals(owner,f.drivers.getValue(2))}}
 @Test fun initiallyUnboundInterfaceIsNotProbedOnClose(){val f=Fake();f.active=6;f.drivers[2]=UsbDriver(error=-61);val s=UsbConfigurationTakeover(f,6);s.prepare();s.close();assertFalse(f.events.any{it.startsWith("bind:")||it.startsWith("config:")})}
 @Test fun busyKernelBindingIsDetachedRetriedAndRestored(){val f=Fake();val s=UsbConfigurationTakeover(f,6);s.prepare();assertEquals(6,f.active);assertEquals(listOf("config:6","bind:2:false","config:6"),f.events.filter{it.startsWith("config:")||it.startsWith("bind:")});s.close();assertEquals(4,f.active);assertEquals("ipheth",f.drivers.getValue(2).name);val count=f.events.size;s.close();assertEquals(count,f.events.size)}
 @Test fun anotherAppIsNeverDetached(){val f=Fake();f.drivers[1]=UsbDriver("usbfs");val s=UsbConfigurationTakeover(f,6);try{s.prepare();fail()}catch(e:IOException){assertTrue(e.message!!.contains("usbfs"))}finally{s.close()};assertFalse(f.events.any{it.startsWith("config:")||it.startsWith("bind:")})}
 @Test fun failedConfigurationDoesNotProceed(){val f=Fake();f.failSwitch=true;val s=UsbConfigurationTakeover(f,6);try{s.prepare();fail()}catch(_:IOException){}finally{s.close()};assertEquals(4,f.active);assertFalse(f.events.any{it.startsWith("bind:")})}
 @Test fun detachFailureIsBounded(){val f=Fake();f.failDetach=true;val s=UsbConfigurationTakeover(f,6);try{s.prepare();fail()}catch(_:IOException){}finally{s.close()};assertEquals(1,f.events.count{it=="config:6"})}
 @Test fun changedOwnershipOnCleanupDoesNotStealInterface(){val f=Fake();val s=UsbConfigurationTakeover(f,6);s.prepare();f.drivers[1]=UsbDriver("usbfs");s.close();assertEquals(6,f.active);assertFalse(f.events.contains("config:4"));assertTrue(f.events.any{it.contains("restore_incomplete")})}
 @Test fun driverQueryFailurePreventsMutation(){val f=Fake();f.drivers[1]=UsbDriver(error=-13);val s=UsbConfigurationTakeover(f,6);try{s.prepare();fail()}catch(_:IOException){}finally{s.close()};assertFalse(f.events.any{it.startsWith("config:")||it.startsWith("bind:")})}
 @Test fun cancelledLeaseCannotStart(){val f=Fake();val s=UsbConfigurationTakeover(f,6);s.close();try{s.prepare();fail()}catch(_:InterruptedException){};assertTrue(f.events.isEmpty())}
 @Test fun retryFailureRestoresDetachedDriver(){val f=Fake();f.failRetry=true;val s=UsbConfigurationTakeover(f,6);try{s.prepare();fail()}catch(_:IOException){}finally{s.close()};assertEquals(4,f.active);assertEquals("ipheth",f.drivers.getValue(2).name);assertEquals(2,f.events.count{it=="config:6"})}
 @Test fun partialDetachFailureRollsBackEarlierInterface(){val f=Fake();f.drivers[1]=UsbDriver("ipheth");f.denyInterface=2;val s=UsbConfigurationTakeover(f,6);try{s.prepare();fail()}catch(_:IOException){}finally{s.close()};assertEquals(4,f.active);assertEquals("ipheth",f.drivers.getValue(1).name);assertTrue(f.events.contains("bind:1:true"));assertEquals(1,f.events.count{it=="config:6"})}
 @Test fun failedRestoreReattachesCurrentConfigurationDriver(){val f=Fake();val s=UsbConfigurationTakeover(f,6);s.prepare();f.drivers[2]=UsbDriver("ipheth");f.failRestore=true;s.close();assertEquals(6,f.active);assertEquals("ipheth",f.drivers.getValue(2).name);assertTrue(f.events.any{it.contains("restore_incomplete")})}
}
