package EShop.lab2

import EShop.lab2.Cart.{AddItem, CancelCheckout, CloseCheckout, ExpireCart, RemoveItem, Storage, StartCheckout}
import EShop.lab2.CartFSM.Status
import akka.actor.LoggingFSM

import scala.concurrent.duration._
import scala.language.postfixOps

object CartFSM {

  object Status extends Enumeration {
    type Status = Value
    val Empty, NonEmpty, InCheckout = Value
  }

}

class CartFSM extends LoggingFSM[Status.Value, Storage] {
  import EShop.lab2.CartFSM.Status._
  override def logDepth = 12
  val cartTimerDuration: FiniteDuration = 1 seconds


  startWith(Empty, Storage.empty)

  when(Empty) {
    ???
  }

  when(NonEmpty, stateTimeout = cartTimerDuration) {
    ???
  }

  when(InCheckout) {
    ???
  }

}
