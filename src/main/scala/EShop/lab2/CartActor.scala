package EShop.lab2

import EShop.lab2.CartActor.{AddItem, CancelCheckout, CheckoutStarted, CloseCheckout, ExpireCart, GetCart, RemoveItem, StartCheckout}
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.Logging

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object CartActor {

  sealed trait Command
  case class AddItem(item: Any)    extends Command
  case class RemoveItem(item: Any) extends Command
  case object ExpireCart           extends Command
  case object StartCheckout        extends Command
  case object CancelCheckout       extends Command
  case object CloseCheckout        extends Command
  case object GetCart             extends Command // command made to make testing easier

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef, cart: Cart) extends Event
  case class ItemAdded(itemId: Any, cart: Cart)                 extends Event
  case class ItemRemoved(itemId: Any, cart: Cart)               extends Event
  case object CartEmptied                                       extends Event
  case object CartExpired                                       extends Event
  case object CheckoutClosed                                    extends Event
  case class CheckoutCancelled(cart: Cart)                      extends Event

  def props: Props = Props(new CartActor())
}

class CartActor extends Actor {

  private val log                                 = Logging(context.system, this)
  val cartTimerDuration: FiniteDuration           = 5 seconds
  implicit val executionContext: ExecutionContext = context.system.dispatcher

  private def scheduleTimer: Cancellable = context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  def receive: Receive = empty

  def empty: Receive = {
    case AddItem(item) => {
      context.become(nonEmpty(new Cart(Seq(item)), scheduleTimer))
    }
    case GetCart => {
      sender ! Cart.empty
    }
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = {
    case AddItem(item) => {
      timer.cancel()
      context.become(nonEmpty(cart.addItem(item), scheduleTimer))
    }
    case RemoveItem(item) if cart.contains(item) && cart.size == 1 => {
      timer.cancel()
      context.become(empty)
    }
    case RemoveItem(item) if cart.contains(item) => {
      timer.cancel()
      context.become(nonEmpty(cart.removeItem(item), scheduleTimer))
    }
    case StartCheckout => {
      timer.cancel()
      val checkoutRef = context.actorOf(Checkout.props(self))
      checkoutRef ! Checkout.StartCheckout
      sender ! CheckoutStarted(checkoutRef, cart)
      context.become(inCheckout(cart))
    }
    case ExpireCart => {
      timer.cancel()
      context.become(empty)
    }
    case GetCart => {
      sender ! Cart.empty
    }
  }

  def inCheckout(cart: Cart): Receive = {
    case CancelCheckout => context.become(nonEmpty(cart, scheduleTimer))
    case CloseCheckout  => context.become(empty)
  }

}
