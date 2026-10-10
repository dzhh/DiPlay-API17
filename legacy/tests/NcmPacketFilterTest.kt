package local.headunit.diplay
import org.junit.Assert.*
import org.junit.Test
class NcmPacketFilterTest {
 @Test fun enablesIpv6MulticastOnControlInterfaceWithBoundedTimeout(){
  assertEquals(0,NcmPacketFilter.configure(3){type,req,value,index,timeout->
   assertEquals(0x21,type);assertEquals(0x43,req);assertEquals(0x0e,value);assertEquals(3,index);assertEquals(250,timeout);0})
 }
 @Test fun unsupportedRequestIsReturnedWithoutRetry(){for(error in listOf(-1,-32,-110)){var calls=0;assertEquals(error,NcmPacketFilter.configure(3){_,_,_,_,_->calls++;error});assertEquals(1,calls)}}
 @Test fun invalidInterfaceCannotSendControlRequest(){for(index in listOf(-1,256)){var calls=0;try{NcmPacketFilter.configure(index){_,_,_,_,_->calls++;0};fail()}catch(_:IllegalArgumentException){};assertEquals(0,calls)}}
}
