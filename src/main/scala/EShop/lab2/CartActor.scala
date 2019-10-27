package EShop.lab2

import EShop.lab2.CartActor.{AddItem, CancelCheckout, CloseCheckout, ExpireCart, RemoveItem, StartCheckout}
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.Logging

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object CartActor {

  sealed trait Command

  case class AddItem(item: Any) extends Command

  case class RemoveItem(item: Any) extends Command

  case object ExpireCart extends Command

  case object StartCheckout extends Command

  case object CancelCheckout extends Command

  case object CloseCheckout extends Command

  sealed trait Event

  case class CheckoutStarted(checkoutRef: ActorRef) extends Event

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
      context.become(inCheckout(cart))
    }
    case ExpireCart => {
      timer.cancel()
      context.become(empty)
    }
  }

  def inCheckout(cart: Cart): Receive = {
    case CancelCheckout => context.become(nonEmpty(cart, scheduleTimer))
    case CloseCheckout  => context.become(empty)

  }

}
