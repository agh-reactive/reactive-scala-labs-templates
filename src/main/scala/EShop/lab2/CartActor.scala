package EShop.lab2

import akka.actor.{Actor, ActorRef,ActorSystem, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

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

  val system: ActorSystem = akka.actor.ActorSystem("system")

  import CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer: Cancellable = system.scheduler.scheduleOnce(delay = cartTimerDuration) {
    self ! ExpireCart
  }

  def receive: Receive = empty

  def empty: Receive = LoggingReceive {
    case AddItem(item) =>
      val cart = Cart(Seq(item))
      context.become(nonEmpty(cart, scheduleTimer))
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case AddItem(item) =>
      timer.cancel
      val updatedCart = cart.addItem(item)
      context.become(nonEmpty(updatedCart,scheduleTimer))
    case RemoveItem(item) =>
      val updatedCart = cart.removeItem(item)
      val removedExistingItem = cart.size != updatedCart.size
      if (removedExistingItem){
        timer.cancel
        if (updatedCart.size == 0){
          context.become(empty)
        }else{
          context.become(nonEmpty(cart = updatedCart, timer = scheduleTimer))
        }
    }
    case ExpireCart =>
      context.become(empty)
    case StartCheckout =>
      context.become(inCheckout(cart = cart))
  
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive{
    case ConfirmCheckoutCancelled =>
      context.become(nonEmpty(cart = cart, timer = scheduleTimer))
    case ConfirmCheckoutClosed =>
      context.become(empty)
  }

}
