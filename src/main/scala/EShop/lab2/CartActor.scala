package EShop.lab2

import EShop.lab2.CartActor._
import EShop.lab2.Checkout.{CheckOutClosed, ExpireCheckout}
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.ExecutionContext.Implicits.global
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

  private def scheduleTimer: Cancellable =
    context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCheckout)

  def receive: Receive = empty

  def empty: Receive = LoggingReceive {
    case AddItem(item: Any) =>
      val cart: Cart = Cart.empty.addItem(item)
      context become nonEmpty(cart, scheduleTimer)
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case ExpireCart =>
      context become empty
    case RemoveItem(item: Any) =>
      if (cart.contains(item)) {
        if (cart.size == 1) {
          context become empty
        } else {
          context become nonEmpty(cart.removeItem(item), scheduleTimer)
        }
      }
    case AddItem(item: Any) =>
      context become nonEmpty(cart.addItem(item), scheduleTimer)
    case StartCheckout =>
      timer.cancel()
      val checkoutRef = context.actorOf(Checkout.props(self))
      //        val checkoutRef = context.actorOf(Checkout.props(self))
      //        checkoutRef ! Checkout.StartCheckout
      //        sender ! CheckoutStarted(checkoutRef)
      CheckoutStarted(checkoutRef)
      context become inCheckout(cart)
    case ExpireCheckout =>
      timer.cancel()
      context become empty
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case CancelCheckout =>
      context become nonEmpty(cart, scheduleTimer)
    case CloseCheckout =>
      context become empty
  }

}
