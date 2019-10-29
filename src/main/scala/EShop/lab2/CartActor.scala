package EShop.lab2

import EShop.lab2.CartActor.{AddItem, CancelCheckout, CloseCheckout, ExpireCart, RemoveItem, StartCheckout}
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {

  sealed trait Command
  case class AddItem(item: Any)    extends Command
  case class RemoveItem(item: Any) extends Command
  case object ExpireCart           extends Command
  case object StartCheckout        extends Command
  case object CancelCheckout       extends Command
  case object CloseCheckout        extends Command

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef) extends Event

  def props = Props(new CartActor())
}

class CartActor extends Actor {

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer =
    context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)(context.system.dispatcher)

  def receive: Receive = empty

  def empty: Receive = LoggingReceive {
    case AddItem(item) => context become nonEmpty(Cart.empty.addItem(item), scheduleTimer)
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case RemoveItem(item) if cart.contains(item) =>
      timer.cancel()
      if (cart.size == 1) {
        cart.removeItem(item)
        context become empty
      } else {
        cart.removeItem(item)
        context become nonEmpty(cart, scheduleTimer)
      }
    case AddItem(item) =>
      timer.cancel()
      context become nonEmpty(cart.addItem(item), scheduleTimer)
    case StartCheckout =>
      timer.cancel()
      context become inCheckout(cart)
    case ExpireCart => context become empty
  }

  def inCheckout(cart: Cart): Receive = {
    case CloseCheckout  => context become empty
    case CancelCheckout => context become nonEmpty(cart, scheduleTimer)
  }

}
