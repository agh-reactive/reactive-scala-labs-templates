package EShop.lab2

import EShop.lab2.Checkout._
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.ExecutionContext.Implicits.global
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
  case object ConfirmPaymentReceived              extends Command

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

  private def checkoutTimer: Cancellable =
    context.system.scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout)
  private def paymentTimer: Cancellable =
    context.system.scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment)

  def receive: Receive = LoggingReceive {
    case StartCheckout =>
      context become selectingDelivery(checkoutTimer)
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive {
    case CancelCheckout | ExpireCheckout =>
      context become cancelled
    case SelectDeliveryMethod(method) =>
      context become selectingPaymentMethod(timer)
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive {
    case CancelCheckout | ExpireCheckout =>
      context become cancelled
    case SelectPayment(payment) =>
      timer.cancel
      context become processingPayment(paymentTimer)
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive {
    case CancelCheckout | ExpirePayment =>
      context become cancelled
    case ConfirmPaymentReceived =>
      timer.cancel
      context become closed
  }

  def cancelled: Receive = LoggingReceive {
    case _ => context stop self
  }

  def closed: Receive = LoggingReceive {
    case _ => context stop self
  }

}
