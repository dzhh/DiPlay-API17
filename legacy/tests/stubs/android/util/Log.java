package android.util;
/** JVM tests only. Never compiled or packaged into the production APK. */
public final class Log {
 public static int i(String tag,String message){return 0;}
 public static int i(String tag,String message,Throwable error){return 0;}
 public static int w(String tag,String message){return 0;}
 public static int w(String tag,String message,Throwable error){return 0;}
 public static int w(String tag,Throwable error){return 0;}
 public static int e(String tag,String message){return 0;}
 public static int e(String tag,String message,Throwable error){return 0;}
 public static int d(String tag,String message){return 0;}
 public static int d(String tag,String message,Throwable error){return 0;}
 public static int v(String tag,String message){return 0;}
 public static boolean isLoggable(String tag,int level){return false;}
 public static String getStackTraceString(Throwable error){java.io.StringWriter out=new java.io.StringWriter();error.printStackTrace(new java.io.PrintWriter(out));return out.toString();}
}
