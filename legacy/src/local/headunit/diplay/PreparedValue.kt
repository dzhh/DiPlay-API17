package local.headunit.diplay
import java.util.concurrent.FutureTask
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException

/** One background preparation per process; a failed value can be retried on the next request. */
class PreparedValue<T>(private val prepare:()->T) {
 private var pending:FutureTask<T>?=null
 @Synchronized private fun task():FutureTask<T>{
  pending?.let{return it}
  val next=FutureTask(Callable{prepare()});pending=next
  Thread(next,"diplay-prepare-identity").apply{isDaemon=true;start()}
  return next
 }
 fun warm(){task()}
 fun get():T {val current=task();try{return current.get()}catch(e:ExecutionException){synchronized(this){if(pending===current)pending=null};throw e.cause?:e}}
}
