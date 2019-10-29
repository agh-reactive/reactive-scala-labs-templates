package EShop.lab2

import EShop.lab2.CartActor.{AddItem, CancelCheckout, CloseCheckout, RemoveItem, StartCheckout}
import EShop.lab2.CartFSM.Status
import akka.actor.{LoggingFSM, Props}

import scala.concurrent.duration._
import scala.language.postfixOps

object CartFSM {

  object Status extends Enumeration {
    type Status = Value
    val Empty, NonEmpty, InCheckout = Value
  }

  def props() = Props(new CartFSM())
}

class CartFSM extends LoggingFSM[Status.Value, Cart] {
  import EShop.lab2.CartFSM.Status._

  // useful for debugging, see: https://doc.akka.io/docs/akka/current/fsm.html#rolling-event-log
  override def logDepth = 12

  val cartTimerDuration: FiniteDuration = 1 seconds

  startWith(Empty, Cart.empty)

  when(Empty) {
    case Event(AddItem(item), cart) =>
      goto(NonEmpty).using(cart.addItem(item))
  }

  when(NonEmpty, stateTimeout = cartTimerDuration) {
    case Event(RemoveItem(item), cart) =>
      if (cart.contains(item)) {
        val newCart = cart.removeItem(item)
        if (newCart.size != 0) {
          log.debug("Item " + item + " removed from the cart => empty")
          stay.using(newCart)
        } else {
          log.debug("Item " + item + " removed from the cart")
          goto(Empty).using(newCart)
        }
      } else {
        log.debug("Trying to remove " + item + ", that is not in the cart")
        stay.using(cart)
      }

    case Event(AddItem(item), cart) =>
      log.debug("Item " + item + " added to the cart")
      stay.using(cart.addItem(item))

    case Event(StartCheckout, cart) =>
      log.debug("Starting checkout => inCheckout")
      goto(InCheckout).using(cart)

    case Event(StateTimeout, _) =>
      log.debug("Time out => empty")
      goto(Empty).using(Cart.empty)
  }

  when(InCheckout) {
    case Event(CancelCheckout, cart) =>
      log.debug("Canceling checkout => nonEmpty")
      goto(NonEmpty).using(cart)

    case Event(CloseCheckout, _) =>
      log.debug("Closing checkout => empty")
      goto(Empty).using(Cart.empty)
  }

}
