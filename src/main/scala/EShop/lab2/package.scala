package EShop
import akka.actor.Cancellable

package object lab2 {
  def timerCancellationAndAction[B](timer: Cancellable)(action: => B): B = {
    timer.cancel()
    action
  }
}
