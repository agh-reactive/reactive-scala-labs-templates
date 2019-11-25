package EShop.lab4

import EShop.lab2.{Cart, Checkout}
import akka.actor.{Cancellable, Props}
import akka.event.Logging
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.concurrent.duration._

object PersistentCartActor {

  def props(persistenceId: String) = Props(new PersistentCartActor(persistenceId))
}

class PersistentCartActor(
  val persistenceId: String
) extends PersistentActor {
  import EShop.lab2.CartActor._
  import context._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5.seconds

  private def scheduleTimer =
    system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)(context.system.dispatcher)

  override def receiveCommand: Receive = empty

  private def updateState(event: Event, timer: Option[Cancellable] = None): Unit = {
    timer.foreach(_.cancel)
    event match {
      case CartExpired | CheckoutClosed | CartEmptied                 => become(empty)
      case CheckoutCancelled(cart)                                    => become(nonEmpty(cart, scheduleTimer))
      case ItemAdded(item, cart)                                      => become(nonEmpty(cart.addItem(item), scheduleTimer))
      case ItemRemoved(item, cart) if cart.containsOnlyThisItem(item) => become(empty)
      case ItemRemoved(item, cart)                                    => become(nonEmpty(cart.removeItem(item), scheduleTimer))
      case CheckoutStarted(_, cart)                                   => become(inCheckout(cart))
    }
  }

  def empty: Receive = {
    case AddItem(item) =>
      persist(ItemAdded(item, Cart.empty))(event => updateState(event, None))
    case GetItems => sender ! Cart.empty
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = {
    case RemoveItem(item) if cart.containsOnlyThisItem(item) =>
      persist(CartEmptied)(event => updateState(event, Some(timer)))
    case RemoveItem(item) if cart.contains(item) =>
      persist(ItemRemoved(item, cart))(event => updateState(event, Some(timer)))
    case AddItem(item) =>
      persist(ItemAdded(item, cart))(event => updateState(event, Some(timer)))
    case StartCheckout =>
      val checkout = context.actorOf(PersistentCheckout.props(self, "persistent-checkout"), "checkout")
      val event    = CheckoutStarted(checkout, cart)
      persist(event) { _ =>
        checkout ! Checkout.StartCheckout
        sender() ! event
        updateState(event, Some(timer))
      }
    case ExpireCart =>
      persist(CartExpired) { event =>
        updateState(event, Some(timer))
      }
    case GetItems => sender ! cart
  }

  def inCheckout(cart: Cart): Receive = {
    case CloseCheckout  => persist(CheckoutClosed)(updateState(_))
    case CancelCheckout => persist(CheckoutCancelled(cart))(updateState(_))
  }

  override def receiveRecover: Receive = {
    case event: Event     => updateState(event)
    case _: SnapshotOffer => log.error("Received unhandled snapshot offer")
  }

}
