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
    case Event(AddItem(item), cart: Cart) => goto(NonEmpty).using(cart.addItem(item))
  }

  when(NonEmpty, stateTimeout = cartTimerDuration) {
    case Event(AddItem(item), cart: Cart) => stay.using(cart.addItem(item))
    case Event(RemoveItem(item), cart: Cart) if cart.contains(item) && cart.size == 1 => goto(Empty).using(cart.removeItem(item))
    case Event(RemoveItem(item), cart: Cart) if cart.contains(item) && cart.size > 1 => goto(NonEmpty).using(cart.removeItem(item))
    case Event(StartCheckout, cart: Cart) => goto(InCheckout).using(cart)
    case Event(StateTimeout, _) => goto(Empty).using(Cart.empty)
  }

  when(InCheckout) {
    case Event(CancelCheckout, cart) => goto(NonEmpty).using(cart)
    case Event(CloseCheckout, _) => goto(Empty).using(Cart.empty)
  }

}
