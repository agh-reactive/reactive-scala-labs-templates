package EShop.lab2

import EShop.lab2.Checkout._
import EShop.lab2.CheckoutFSM.Status
import akka.actor.{ActorRef, Cancellable, LoggingFSM, Props}

import scala.concurrent.duration._
import scala.language.postfixOps

object CheckoutFSM {

  def props(cartActor: ActorRef) = Props(new CheckoutFSM)

  object Status extends Enumeration {
    type Status = Value
    val NotStarted, SelectingDelivery, SelectingPaymentMethod, Cancelled, ProcessingPayment, Closed = Value
  }
}

class CheckoutFSM extends LoggingFSM[Status.Value, Data] {
  import EShop.lab2.CheckoutFSM.Status._
  import context.dispatcher

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds
  private val scheduler                     = context.system.scheduler

  // useful for debugging, see: https://doc.akka.io/docs/akka/current/fsm.html#rolling-event-log
  override def logDepth = 12

  private def checkoutTimer: Cancellable = scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout)
  private def paymentTimer: Cancellable  = scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment)

  startWith(NotStarted, Uninitialized)

  when(NotStarted) {
    case Event(StartCheckout, _) => goto(SelectingDelivery) using SelectingDeliveryStarted(checkoutTimer)
  }

  when(SelectingDelivery) {
    case Event(CancelCheckout, SelectingDeliveryStarted(timer)) => timerCancellationAndAction(timer)(goto(Cancelled))
    case Event(ExpireCheckout, SelectingDeliveryStarted(_))     => goto(Cancelled)
    case Event(SelectDeliveryMethod(_), SelectingDeliveryStarted(timer)) =>
      timerCancellationAndAction(timer)(goto(SelectingPaymentMethod) using SelectingDeliveryStarted(checkoutTimer))
  }

  when(SelectingPaymentMethod) {
    case Event(CancelCheckout, SelectingDeliveryStarted(timer)) => timerCancellationAndAction(timer)(goto(Cancelled))
    case Event(ExpireCheckout, SelectingDeliveryStarted(_))     => goto(Cancelled)
    case Event(SelectPayment(_), SelectingDeliveryStarted(timer)) =>
      timerCancellationAndAction(timer)(goto(ProcessingPayment) using ProcessingPaymentStarted(paymentTimer))
  }

  when(ProcessingPayment) {
    case Event(CancelCheckout, ProcessingPaymentStarted(timer)) => timerCancellationAndAction(timer)(goto(Cancelled))
    case Event(ExpirePayment, _)                                => goto(Cancelled)
    case Event(ReceivePayment, ProcessingPaymentStarted(timer)) => timerCancellationAndAction(timer)(goto(Closed))
  }

  when(Cancelled) {
    case _ =>
      log.info("Checkout has been already cancelled")
      stay()
  }

  when(Closed) {
    case _ =>
      log.info("Checkout has been already closed")
      stay()
  }

}
