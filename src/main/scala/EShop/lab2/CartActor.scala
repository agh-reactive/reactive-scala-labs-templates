package EShop.lab2

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {

  sealed trait Command
  case class AddItem(item: Any)        extends Command
  case class RemoveItem(item: Any)     extends Command
  case object ExpireCart               extends Command
  case object StartCheckout            extends Command
  case object ConfirmCheckoutCancelled extends Command
  case object ConfirmCheckoutClosed    extends Command

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef) extends Event

  def props = Props(new CartActor())
}

class CartActor extends Actor {

  import CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer: Cancellable = context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  def receive: Receive = empty

  def empty: Receive = {
    case AddItem(item) =>
      context become nonEmpty(Cart(Seq(item)), scheduleTimer)
      scheduleTimer
    case ExpireCart =>
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = {
    case AddItem(item) =>
      context become nonEmpty(cart.addItem(item), timer)
    case RemoveItem(item) if cart.contains(item) =>
      if (cart.size != 1) {
        context become nonEmpty(cart.removeItem(item), timer)
      } else {
        context become empty
      }
    case StartCheckout =>
      context become inCheckout(cart)
    case ExpireCart =>
      context become empty
  }

  def inCheckout(cart: Cart): Receive = {
    case ConfirmCheckoutCancelled =>
      context become nonEmpty(cart, scheduleTimer)
    case ConfirmCheckoutClosed =>
      context become empty
  }

}
