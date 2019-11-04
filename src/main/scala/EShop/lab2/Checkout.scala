package EShop.lab2

import EShop.lab2.Checkout._
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.ExecutionContextExecutor
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

  implicit val ec: ExecutionContextExecutor      = context.dispatcher
  private def scheduleCheckoutTimer: Cancellable = scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout)
  private def schedulePaymentTimer: Cancellable  = scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment)

  def receive: Receive = LoggingReceive.withLabel("State: receive") {
    case StartCheckout => context become selectingDelivery(scheduleCheckoutTimer)
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive.withLabel("[State: selectingDelivery]") {
    case CancelCheckout => timerCancellationAndAction(timer)(context become cancelled)
    case ExpireCheckout => context become cancelled
    case SelectDeliveryMethod(method) =>
      timerCancellationAndAction(timer)(context become selectingPaymentMethod(scheduleCheckoutTimer))
  }

  def selectingPaymentMethod(timer: Cancellable): Receive =
    LoggingReceive.withLabel("[State: selectingPaymentMethod]") {
      case CancelCheckout => timerCancellationAndAction(timer)(context become cancelled)
      case ExpireCheckout => context become cancelled
      case SelectPayment(payment) =>
        timerCancellationAndAction(timer)(context become processingPayment(schedulePaymentTimer))
    }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive.withLabel("[State: processingPayment]") {
    case CancelCheckout => timerCancellationAndAction(timer)(context become cancelled)
    case ExpirePayment  => context become cancelled
    case ReceivePayment => timerCancellationAndAction(timer)(context become closed)
  }

  def cancelled: Receive = LoggingReceive.withLabel("[State: cancelled]") {
    case _ => log.info("Checkout already cancelled")
  }

  def closed: Receive = LoggingReceive.withLabel("[State: closed]") {
    case _ => log.info("Checkout already closed")
  }

}
