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
  case object GetItems                 extends Command // command made to make testing easier

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

  import CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer: Cancellable = ???

  def receive: Receive = ???

  def empty: Receive = ???

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = ???

  def inCheckout(cart: Cart): Receive = ???

}
