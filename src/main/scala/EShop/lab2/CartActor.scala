package EShop.lab2

import EShop.lab2.CartActor._
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {

  def props = Props(new CartActor())

  sealed trait Command

  sealed trait Event

  case class AddItem(item: Any) extends Command

  case class RemoveItem(item: Any) extends Command

  case class CheckoutStarted(checkoutRef: ActorRef) extends Event

  case object ExpireCart extends Command

  case object StartCheckout extends Command

  case object CancelCheckout extends Command

  case object CloseCheckout extends Command
}

class CartActor extends Actor {
  val cartTimerDuration                     = 5 seconds
  private val log                           = Logging(context.system, this)
  implicit val ec: ExecutionContextExecutor = context.dispatcher

  def receive: Receive = empty

  def empty: Receive = LoggingReceive.withLabel("[State: empty]") {
    case AddItem(item) =>
      context become nonEmpty(Cart.empty.addItem(item), scheduleTimer)
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive.withLabel("[State: nonEmpty]") {
    case AddItem(item) =>
      timerCancellationAndAction(timer)(context become nonEmpty(cart.addItem(item), scheduleTimer))
    case RemoveItem(item) if cart.contains(item) && cart.size == 1 =>
      timerCancellationAndAction(timer)(context become empty)
    case RemoveItem(item) if cart.contains(item) =>
      timerCancellationAndAction(timer)(context become nonEmpty(cart.removeItem(item), scheduleTimer))
    case StartCheckout => timerCancellationAndAction(timer)(context become inCheckout(cart))
    case ExpireCart    => context become empty
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive.withLabel("[State: inCheckout]") {
    case CancelCheckout => context become nonEmpty(cart, scheduleTimer)
    case CloseCheckout  => context become empty
  }

  private def scheduleTimer: Cancellable = context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

}
