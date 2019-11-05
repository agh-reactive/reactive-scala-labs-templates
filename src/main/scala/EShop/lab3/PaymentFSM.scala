package EShop.lab3

import EShop.lab2.Checkout.ReceivePayment
import EShop.lab3.Payment._
import akka.actor.{ActorRef, FSM, Props}

object PaymentFSM {
  def props(method: String, orderManager: ActorRef, checkout: ActorRef) =
    Props(new PaymentFSM(method, orderManager, checkout))

}

class PaymentFSM(
  method: String,
  orderManager: ActorRef,
  checkout: ActorRef
) extends FSM[State, Data] {

  startWith(WaitingForPayment, Empty)

  when(WaitingForPayment) {
    case Event(DoPayment, _) => {
      checkout ! ReceivePayment
      orderManager ! PaymentConfirmed
      context.stop(self)
      stay
    }
  }

}
