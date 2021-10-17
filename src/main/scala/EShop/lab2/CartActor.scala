package EShop.lab2

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.ActorContext

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

  private def addItemAndChangeContext(item: Any, context: ActorContext): Unit = {
    val cartWithItem = Cart.empty.addItem(item)
    context become nonEmpty(cartWithItem, scheduleTimer)
  }
  def receive: Receive = { case AddItem(item) =>
    addItemAndChangeContext(item, context)
  }

  def empty(): Receive = { case AddItem(item) =>
    val cart = Cart.empty.addItem(item)
    context become nonEmpty(cart, scheduleTimer)
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = {
    case AddItem(item) =>
      val updatedCart = cart.addItem(item)
      context become nonEmpty(updatedCart, scheduleTimer)
    case RemoveItem(item) if cart.size > 0 && cart.contains(item) =>
      val updatedCart: Cart = cart.removeItem(item)
      if (updatedCart.size == 0) {
        timer.cancel()
        context become empty
      }
      else context become nonEmpty(updatedCart, scheduleTimer)

    case StartCheckout =>
      timer.cancel()
      context become inCheckout(cart)
    case ExpireCart =>
      timer.cancel()
      context become empty
  }

  def inCheckout(cart: Cart): Receive = {
    case ConfirmCheckoutCancelled =>
      context become nonEmpty(cart, scheduleTimer)
    case ConfirmCheckoutClosed =>
      context become empty
  }

}