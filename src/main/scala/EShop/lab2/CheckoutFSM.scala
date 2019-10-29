package EShop.lab2

import EShop.lab2.Checkout.{
  CancelCheckout,
  Data,
  ExpireCheckout,
  ExpirePayment,
  ProcessingPaymentStarted,
  ReceivePayment,
  SelectDeliveryMethod,
  SelectPayment,
  SelectingDeliveryStarted,
  StartCheckout,
  Uninitialized
}
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
    case Event(StartCheckout, _) =>
      val timer = scheduler.scheduleOnce(delay = checkoutTimerDuration, receiver = self, message = ExpireCheckout)(
        context.system.dispatcher
      )
      goto(SelectingDelivery) using SelectingDeliveryStarted(timer)
  }

  when(SelectingDelivery) {
    case Event(SelectDeliveryMethod(_), _) => goto(SelectingPaymentMethod)
    case Event(CancelCheckout, _)          => goto(Cancelled)
    case Event(ExpireCheckout, _)          => goto(Cancelled)
  }

  when(SelectingPaymentMethod) {
    case Event(SelectPayment(_), SelectingDeliveryStarted(timer)) =>
      timer.cancel()
      val newTimer = scheduler.scheduleOnce(delay = checkoutTimerDuration, receiver = self, message = ExpireCheckout)(
        context.system.dispatcher
      )
      goto(ProcessingPayment) using ProcessingPaymentStarted(newTimer)
    case Event(CancelCheckout, _) => goto(Cancelled)
    case Event(ExpireCheckout, _) => goto(Cancelled)
  }

  when(ProcessingPayment) {
    case Event(ReceivePayment, _) => goto(Closed)
    case Event(CancelCheckout, _) => goto(Cancelled)
    case Event(ExpireCheckout, _) => goto(Cancelled)
    case Event(ExpirePayment, _)  => goto(Cancelled)
  }

  when(Cancelled) {
    case _ => stay
  }

  when(Closed) {
    case _ => stay
  }

}
