package EShop.lab2

import akka.actor.{Actor, ActorRef, Cancellable, Props, Timers}
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

class Checkout extends Actor with Timers {
  import Checkout._

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)
  private val checkoutTimerDuration = 1 seconds
  private val paymentTimerDuration  = 1 seconds

  def receive: Receive = LoggingReceive{
    case StartCheckout => context become selectingDelivery(scheduler.scheduleOnce(checkoutTimerDuration) {
      self ! ExpireCheckout
    })
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive{
    case SelectDeliveryMethod(method) =>
      timer.cancel()
      context become selectingPaymentMethod(scheduler.scheduleOnce(paymentTimerDuration) {
        context.self ! ExpirePayment
      })
    case CancelCheckout =>
      context become cancelled
    case ExpireCheckout =>
      context become cancelled
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive{
    case SelectPayment(payment) =>
      log.info("Selected $payment payment method")
      timer.cancel()

    case ExpirePayment =>
      context become cancelled
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive{
    case PaymentStarted(actorRef) =>
    case ConfirmPaymentReceived =>
      timer.cancel()
      context become closed
    case CancelCheckout => context become cancelled
  }

  def cancelled: Receive = ???

  def closed: Receive = LoggingReceive{

  }

}
