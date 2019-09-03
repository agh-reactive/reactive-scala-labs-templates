package EShop.lab3

import EShop.lab3.OrderManager._
import akka.actor.FSM

class OrderManagerFSM extends FSM[State, Data] {

  startWith(Uninitialized, Empty)

  when(Uninitialized) {
    ???
  }

  when(Open) {
    ???
  }

  when(InCheckout) {
    ???
  }

  when(InPayment) {
    ???
  }

  when(Finished) {
    case _ =>
      sender ! "order manager finished job"
      stay()
  }

}
