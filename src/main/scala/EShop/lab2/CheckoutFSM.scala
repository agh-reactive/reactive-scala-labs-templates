package EShop.lab2

import EShop.lab2.Checkout._
import EShop.lab2.CheckoutFSM.Status
import akka.actor.{ActorRef, LoggingFSM, Props}

import scala.concurrent.duration._
import scala.language.postfixOps

object CheckoutFSM {

  object Status extends Enumeration {
    type Status = Value
    val NotStarted, SelectingDelivery, SelectingPaymentMethod, Cancelled, ProcessingPayment, Closed = Value
  }

  def props(cartActor: ActorRef) = Props(new CheckoutFSM)
}

class CheckoutFSM extends LoggingFSM[Status.Value, Data] {
  import EShop.lab2.CheckoutFSM.Status._

  // useful for debugging, see: https://doc.akka.io/docs/akka/current/fsm.html#rolling-event-log
  override def logDepth = 12

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  private val scheduler = context.system.scheduler

  startWith(NotStarted, Uninitialized)

  when(NotStarted) {
    case Event(StartCheckout, s) =>
      setTimer("checkoutTimer", ExpireCheckout, checkoutTimerDuration, false)
      goto(SelectingDelivery).using(s)
  }

  when(SelectingDelivery) {
    case Event(SelectDeliveryMethod(method), s) =>
      goto(SelectingPaymentMethod).using(s)

    case Event(CancelCheckout, s) =>
      cancelTimer("checkoutTimer")
      goto(Cancelled).using(s)

    case Event(ExpireCheckout, s) =>
      goto(Cancelled).using(s)
  }

  when(SelectingPaymentMethod) {
    case Event(SelectPayment(payment), s) =>
      cancelTimer("checkoutTimer")
      setTimer("paymentTimer", ExpirePayment, paymentTimerDuration, false)
      goto(ProcessingPayment).using(s)

    case Event(CancelCheckout, s) =>
      cancelTimer("checkoutTimer")
      goto(Cancelled).using(s)

    case Event(ExpireCheckout, s) =>
      goto(Cancelled).using(s)
  }

  when(ProcessingPayment) {
    case Event(ReceivePayment, s) =>
      cancelTimer("paymentTimer")
      goto(Closed).using(s)

    case Event(CancelCheckout, s) =>
      cancelTimer("paymentTimer")
      goto(Cancelled).using(s)

    case Event(ExpirePayment, s) =>
      goto(Cancelled).using(s)
  }

  when(Cancelled) {
    case Event(StartCheckout, s) =>
      goto(SelectingDelivery).using(s)
  }

  when(Closed) {
    case Event(StartCheckout, s) =>
      goto(SelectingDelivery).using(s)
  }
}
