package EShop.lab2

import EShop.lab2.CartActor.{
  AddItem,
  CancelCheckout,
  CheckoutStarted,
  CloseCheckout,
  ExpireCart,
  GetItems,
  RemoveItem,
  StartCheckout
}
import EShop.lab3.OrderManager.Done
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
  case object GetItems             extends Command // command made to make testing easier

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef, cart: Cart) extends Event
  case class ItemAdded(itemId: Any, cart: Cart)                 extends Event
  case class ItemRemoved(itemId: Any, cart: Cart)               extends Event
  case object CartEmptied                                       extends Event
  case object CartExpired                                       extends Event
  case object CheckoutClosed                                    extends Event
  case class CheckoutCancelled(cart: Cart)                      extends Event

  def props() = Props(new CartActor())
}

class CartActor extends Actor {

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer =
    context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)(context.system.dispatcher)

  def receive: Receive = empty

  def empty: Receive = LoggingReceive.withLabel("==Empty==") {
    case AddItem(item) => {
      scheduleTimer.cancel()
      context become nonEmpty(Cart.empty.addItem(item), scheduleTimer)
    }
    case GetItems =>
      sender ! Cart.empty
      context become empty
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive.withLabel("==Non Empty==") {
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
      val checkoutActor = context.actorOf(Checkout.props(self), "checkoutActor")
      checkoutActor ! Checkout.StartCheckout
      sender ! CheckoutStarted(checkoutActor, cart)
      context become (inCheckout(cart))
    case ExpireCart => context become empty
    case GetItems =>
      sender ! cart
      context become nonEmpty(cart, timer)
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive.withLabel("==In Checkout==") {
    case CloseCheckout  => context become empty
    case CancelCheckout => context become nonEmpty(cart, scheduleTimer)
  }

}
