package local.headunit.diplay
import android.content.Context
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileDescriptor
object PlatformSupport {
 fun prepareMfi(context:Context):File {val dir=File(context.filesDir,"offline-mfi").apply{mkdirs()};for(name in arrayOf("identity.pk8","certificate.p7b")){val dest=File(dir,name);if(!dest.isFile)context.assets.open("offline-mfi/$name").use{input->dest.outputStream().use{input.copyTo(it)}}};return dir}
 @JvmStatic fun setNonBlocking(p:ParcelFileDescriptor)=setBlockingFlag(p,false)
 @JvmStatic fun setBlocking(p:ParcelFileDescriptor)=setBlockingFlag(p,true)
 private fun setBlockingFlag(p:ParcelFileDescriptor,blocking:Boolean){val os=Class.forName("libcore.io.Libcore").getField("os").get(null);val type=Class.forName("libcore.io.Os");val fd=p.fileDescriptor;val flags=type.getMethod("fcntlVoid",FileDescriptor::class.java,Int::class.javaPrimitiveType).invoke(os,fd,3) as Int;type.getMethod("fcntlLong",FileDescriptor::class.java,Int::class.javaPrimitiveType,Long::class.javaPrimitiveType).invoke(os,fd,4,(if(blocking)flags and 2048.inv() else flags or 2048).toLong());val after=type.getMethod("fcntlVoid",FileDescriptor::class.java,Int::class.javaPrimitiveType).invoke(os,fd,3) as Int;check((after and 2048==0)==blocking)}
}
