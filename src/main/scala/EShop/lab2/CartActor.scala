package EShop.lab2

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {

  sealed trait Command

  case class AddItem(item: Any) extends Command

  case class RemoveItem(item: Any) extends Command

  case object ExpireCart extends Command

  case object StartCheckout extends Command

  case object ConfirmCheckoutCancelled extends Command
  case object ConfirmCheckoutClosed    extends Command
  case object GetItems                 extends Command // command made to make testing easier

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef) extends Event

  def props() = Props(new CartActor())
}

class CartActor extends Actor {

  import CartActor._

  private val log = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer: Cancellable = context.system.scheduler.scheduleOnce(cartTimerDuration){
    self ! ExpireCart
  }

  def receive: Receive = empty

  def empty: Receive = LoggingReceive {
    case AddItem(item) =>
      log.info("Created empty cart")
      context become nonEmpty(Cart(Seq(item)), scheduleTimer)
    case ExpireCart =>

    case GetItems => sender !  Seq.empty[String]
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case AddItem(item) =>
      log.info(s"Added $item to cart")
      context become nonEmpty(cart.addItem(item), scheduleTimer)
    case RemoveItem(item) =>
      if (cart.size != 1) {
        context become nonEmpty(cart.removeItem(item), timer)
        log.info(s"Removed $item from cart")
      } else if (cart.contains(item)) {
        context become empty
      }

    case StartCheckout =>
      context become inCheckout(cart)
      val checkout = context.system.actorOf(Props(new Checkout(self)), "checkout")
      checkout ! Checkout.StartCheckout
      sender ! CheckoutStarted(checkout)

    case ExpireCart =>
      log.info("Cart has expired")
      context become empty
    case GetItems =>
      sender ! cart.items
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case ConfirmCheckoutCancelled =>
      log.info("Checkout cancelled")
      sender ! ConfirmCheckoutCancelled
      context become nonEmpty(cart, scheduleTimer)
    case ConfirmCheckoutClosed =>
      log.info("Checkout completed")
      sender ! ConfirmCheckoutClosed
      context become empty
    case GetItems =>
      sender ! cart.items
  }
}
