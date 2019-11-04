package EShop.lab4

import EShop.lab2.{Cart, Checkout}
import akka.actor.{Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.PersistentActor

import scala.concurrent.duration._

object PersistentCartActor {

  def props(persistenceId: String) = Props(new PersistentCartActor(persistenceId))
}

class PersistentCartActor(
  val persistenceId: String
) extends PersistentActor {

  import EShop.lab2.CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5.seconds

  private def scheduleTimer: Cancellable = ???

  override def receiveCommand: Receive = empty

  private def updateState(event: Event, timer: Option[Cancellable] = None): Unit = {
    ???
    event match {
      case CartExpired | CheckoutClosed       => ???
      case CheckoutCancelled(cart)            => ???
      case ItemAdded(item, cart)              => ???
      case CartEmptied                        => ???
      case ItemRemoved(item, cart)            => ???
      case CheckoutStarted(checkoutRef, cart) => ???
    }
  }

  def empty: Receive = ???

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = ???

  def inCheckout(cart: Cart): Receive = ???

  override def receiveRecover: Receive = ???
}
