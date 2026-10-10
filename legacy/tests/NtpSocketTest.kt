package local.headunit.diplay

import com.shilapi.xcertplay.airplay.NtpClock
import java.net.*
import org.junit.Assert.*
import org.junit.Test

class NtpSocketTest {
 @Test fun repeatedListenReusesSocketAndCloseReleasesPort() {
  val address=InetAddress.getByName("127.0.0.1");val clock=NtpClock()
  val port=try{val p=clock.listen(address);assertEquals(p,clock.listen(address));p}finally{clock.close()}
  val rebound=DatagramSocket(null)
  try{rebound.bind(InetSocketAddress(address,port));assertEquals(port,rebound.localPort)}finally{rebound.close()}
 }
 @Test fun respondsToTimingPacket() {
  val address=InetAddress.getByName("127.0.0.1");val clock=NtpClock();val peer=LegacySockets.udp(address)
  try {
   val port=clock.listen(address);peer.soTimeout=2000
   val request=ByteArray(32);request[1]=210.toByte();request[31]=71
   peer.send(DatagramPacket(request,32,address,port))
   val response=DatagramPacket(ByteArray(64),64);peer.receive(response)
   assertEquals(32,response.length);assertEquals(211,response.data[1].toInt() and 255);assertEquals(71,response.data[15].toInt())
  }finally{peer.close();clock.close()}
 }
 @Test fun invalidTimingPortRejected() {
  val clock=NtpClock()
  try{for(port in listOf(-1,0,65536)){try{clock.start(InetAddress.getByName("127.0.0.1"),port);fail("Invalid port accepted")}catch(expected:IllegalArgumentException){}}}finally{clock.close()}
 }
}
