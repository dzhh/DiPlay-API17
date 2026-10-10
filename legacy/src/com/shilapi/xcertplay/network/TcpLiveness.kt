package com.shilapi.xcertplay.network
import java.net.Socket
internal object TcpLiveness { fun configure(socket:Socket, diagnostic:(String)->Unit) { socket.keepAlive=true } }
