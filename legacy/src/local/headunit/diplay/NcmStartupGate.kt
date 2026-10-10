package local.headunit.diplay

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/** Hold outbound traffic until the CarPlay start message is on the control channel.
 * Closing must wake an unauthenticated session without releasing a packet. */
class NcmStartupGate {
 private val ready=CountDownLatch(1)
 private val closed=AtomicBoolean(false)
 fun start(){ready.countDown()}
 fun close(){closed.set(true);ready.countDown()}
 fun await():Boolean {ready.await();return !closed.get()}
}
