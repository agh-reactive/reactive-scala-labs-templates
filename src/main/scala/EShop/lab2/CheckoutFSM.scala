package EShop.lab2

import EShop.lab2.Checkout.{CancelCheckout, Data, ExpireCheckout, ExpirePayment, ProcessingPaymentStarted, ReceivePayment, SelectDeliveryMethod, SelectPayment, SelectingDeliveryStarted, StartCheckout, Uninitialized}
import akka.actor.LoggingFSM

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import EShop.lab2.CheckoutFSM.Status

object CheckoutFSM {

  object Status extends Enumeration {
    type Status = Value
    val NotStarted, SelectingDelivery, SelectingPaymentMethod, Cancelled, ProcessingPayment, Closed = Value
  }

}

class CheckoutFSM extends LoggingFSM[Status.Value, Data] {
  import EShop.lab2.CheckoutFSM.Status._
  override def logDepth = 12
  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration = 1 seconds

  private val scheduler = context.system.scheduler

  startWith(NotStarted, Uninitialized)

  when(NotStarted) {
    ???
  }

  when(SelectingDelivery) {
    ???
  }

  when(SelectingPaymentMethod) {
    ???
  }

  when(ProcessingPayment) {
    ???
  }

  when(Cancelled) {
    ???
  }

  when(Closed) {
    ???
  }

}
