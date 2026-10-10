package local.headunit.diplay;
import java.io.Closeable;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

/** Bound the whole operation as well as each socket read; owned by exactly one request. */
final class NetworkDeadline implements Closeable {
 private final HttpURLConnection connection;
 private final Timer timer=new Timer("diplay-http-deadline",true);
 private volatile boolean expired;
 private final java.util.concurrent.atomic.AtomicBoolean cancelled=new java.util.concurrent.atomic.AtomicBoolean();
 private final java.util.concurrent.atomic.AtomicBoolean disconnecting=new java.util.concurrent.atomic.AtomicBoolean();
 private final java.util.concurrent.atomic.AtomicBoolean abortScheduled=new java.util.concurrent.atomic.AtomicBoolean();
 NetworkDeadline(final HttpURLConnection connection,long milliseconds){
  this.connection=connection;
  timer.schedule(new TimerTask(){public void run(){expired=true;scheduleAbort();}},milliseconds);
 }
 private void scheduleAbort(){
  // API17 may accept a TCP socket before HttpEngine publishes its connection.
  // A single disconnect in that gap is a no-op; keep aborting until the owner closes.
  // Healthy transfers have no polling wakeups. close() cancels both timer tasks.
  if(abortScheduled.compareAndSet(false,true))try{timer.schedule(new TimerTask(){public void run(){disconnect();}},0,100);}catch(IllegalStateException alreadyClosed){}
 }
 private void disconnect(){if(disconnecting.compareAndSet(false,true))try{connection.disconnect();}catch(RuntimeException ignored){}finally{disconnecting.set(false);}}
 void check()throws InterruptedIOException{if(cancelled.get())throw new InterruptedIOException("Request cancelled");if(expired)throw new SocketTimeoutException("Request deadline exceeded");}
 void cancel(){cancelled.set(true);scheduleAbort();}
 public void close(){timer.cancel();disconnect();}
}
