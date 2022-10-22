package EShop.lab2

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

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

  private def scheduleTimer: Cancellable = {
    context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)(context.system.dispatcher)
  }

  def receive: Receive = LoggingReceive {
    case AddItem(item) =>
      context become nonEmpty(Cart(Seq(item)), scheduleTimer)
  }

  def empty: Receive = LoggingReceive {
    case AddItem(item) =>
      context become nonEmpty(Cart(Seq(item)), scheduleTimer)
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case AddItem(item) =>
      timer.cancel()
      context become nonEmpty(cart.addItem(item), scheduleTimer)
    case RemoveItem(item) =>
      timer.cancel()
      if (cart.contains(item)){
        if (cart.size > 1)
          context become nonEmpty(cart.removeItem(item), scheduleTimer)
        else
          context become empty
      }
    case StartCheckout =>
      timer.cancel()
      context become inCheckout(cart)
    case ExpireCart =>
      context become empty
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case ConfirmCheckoutCancelled =>
      log.log(Logging.InfoLevel, "Your cart has return to its previous state")
      context become nonEmpty(cart, scheduleTimer)
    case ConfirmCheckoutClosed =>
      log.log(Logging.InfoLevel, "Your cart is now empty")
      context become empty
  }

}
