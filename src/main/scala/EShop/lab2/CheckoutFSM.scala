package EShop.lab2

import EShop.lab2.Checkout.{CancelCheckout, Data, ExpireCheckout, ReceivePayment, SelectDeliveryMethod, SelectPayment, SelectingDeliveryStarted, StartCheckout, Uninitialized}
import EShop.lab2.CheckoutFSM.Status
import akka.actor.{ActorRef, LoggingFSM, Props}

import scala.concurrent.duration._
import scala.language.postfixOps

object CheckoutFSM {
  object Status extends Enumeration {
    type Status = Value
    val NotStarted, SelectingDelivery, SelectingPaymentMethod, Cancelled, ProcessingPayment, Closed = Value
  }

  def props(cartActor: ActorRef) = Props(new CheckoutFSM(cartActor))
}

class CheckoutFSM(cartActor: ActorRef) extends LoggingFSM[Status.Value, Data] {
  import EShop.lab2.CheckoutFSM.Status._

  // useful for debugging, see: https://doc.akka.io/docs/akka/current/fsm.html#rolling-event-log
  override def logDepth = 12

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  private val scheduler = context.system.scheduler

  startWith(NotStarted, Uninitialized)

  when(NotStarted) {
    case Event(StartCheckout, Uninitialized) => goto(SelectingDelivery)
  }

  when(SelectingDelivery, stateTimeout = checkoutTimerDuration) {
    case Event(SelectDeliveryMethod(_), _) => goto(SelectingPaymentMethod)
    case Event(CancelCheckout | ExpireCheckout | StateTimeout, _) => goto(Cancelled)
  }

  when(SelectingPaymentMethod, stateTimeout = checkoutTimerDuration) {
    case Event(SelectPayment(_), _) => goto(ProcessingPayment)
    case Event(CancelCheckout | ExpireCheckout | StateTimeout, _) => goto(Cancelled)
  }

  when(ProcessingPayment, stateTimeout = paymentTimerDuration) {
    case Event(ReceivePayment, _) => goto(Closed)
    case Event(CancelCheckout | ExpireCheckout | StateTimeout, _) => goto(Cancelled)
  }

  when(Cancelled) {
    case _ => stay()
  }

  when(Closed) {
    case _ => stay()
  }
}
