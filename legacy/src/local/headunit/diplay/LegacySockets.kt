package local.headunit.diplay

import java.net.*

/** Android 4.2 IoBridge.bind mistakes every unscoped Inet6Address for link-local.
 * Bind the established control connection's local address, never an unscoped ::.
 * IPv4 ANY is only the default for standalone callers; Android uses dual-stack sockets.
 */
object LegacySockets {
 fun anyAddress(): InetAddress = InetAddress.getByName("0.0.0.0")
 fun udp(address: InetAddress = anyAddress()): DatagramSocket {
  val socket = DatagramSocket(null)
  try { socket.reuseAddress=true;socket.bind(InetSocketAddress(address,0));return socket }
  catch(e:Exception){socket.close();throw e}
 }
 fun tcp(address: InetAddress = anyAddress()): ServerSocket {
  val socket=ServerSocket()
  try { socket.reuseAddress=true;socket.bind(InetSocketAddress(address,0));return socket }
  catch(e:Exception){socket.close();throw e}
 }
}
