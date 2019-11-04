package EShop
import akka.actor.Cancellable

package object lab2 {
  def timerCancellationAndAction(timer: Cancellable)(action: => Unit): Unit = {
    timer.cancel()
    action
  }
}
