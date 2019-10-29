package EShop.lab2

import EShop.lab2.Checkout.{
  CancelCheckout,
  ExpireCheckout,
  ExpirePayment,
  ReceivePayment,
  SelectDeliveryMethod,
  SelectPayment,
  StartCheckout
}
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.language.postfixOps

object Checkout {

  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ReceivePayment                      extends Command

  sealed trait Event
  case object CheckOutClosed                   extends Event
  case class PaymentStarted(payment: ActorRef) extends Event

  def props(cart: ActorRef) = Props(new Checkout())
}

class Checkout extends Actor {

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration = 1 seconds
  val paymentTimerDuration  = 1 seconds

  def receive: Receive = LoggingReceive.withLabel("In checkout proceeding to selecting delivery.") {
    case StartCheckout => {
      val checkoutTimer =
        scheduler.scheduleOnce(checkoutTimerDuration, receiver = self, ExpireCheckout)(context.system.dispatcher)
      context become selectingDelivery(checkoutTimer)
    }
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive.withLabel("In delivery selection.") {
    case SelectDeliveryMethod(_) => context become selectingPaymentMethod(timer)
    case CancelCheckout          => context become cancelled
    case ExpireCheckout          => context become cancelled
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = {
    case SelectPayment(_) =>
      timer.cancel()
      val payTimer =
        scheduler.scheduleOnce(checkoutTimerDuration, receiver = self, ExpireCheckout)(context.system.dispatcher)
      context become processingPayment(payTimer)
    case CancelCheckout => context become cancelled
    case ExpireCheckout => context become cancelled
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive.withLabel("In payment processing.") {
    case ReceivePayment =>
      timer.cancel()
      context become closed
    case CancelCheckout => context become cancelled
    case ExpireCheckout => context become cancelled
  }

  def cancelled: Receive = LoggingReceive.withLabel("Payment cancelled.") {
    case msg => log.error("Payment cancelled", msg.toString)
  }

  def closed: Receive = LoggingReceive.withLabel("Payment closed") {
    case msg => log.error("Payment closed", msg.toString)
  }

}
