package EShop.lab2

import EShop.lab2.Checkout._
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global

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

}

class Checkout extends Actor {

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration = 1 seconds
  val paymentTimerDuration  = 1 seconds

  private def scheduleCheckoutTimer: Cancellable =
    scheduler.scheduleOnce(delay = checkoutTimerDuration) {
      self ! ExpireCheckout
  }

  private def schedulePaymentTimer: Cancellable =
    scheduler.scheduleOnce(delay = paymentTimerDuration) {
      self ! ExpirePayment
  }
  def receive: Receive = LoggingReceive {
    case StartCheckout =>
      context.become(selectingDelivery(timer = scheduleCheckoutTimer))
  }


  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive {
    case SelectDeliveryMethod(method) =>
      context.become(selectingPaymentMethod(timer = timer))
    case CancelCheckout | ExpireCheckout  =>
      context.become(cancelled)

  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive {
    case SelectPayment(payment) =>
      timer.cancel
      context.become(processingPayment(timer = schedulePaymentTimer))

    case CancelCheckout | ExpireCheckout =>
      context.become(cancelled)
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive {
    case ConfirmPaymentReceived =>
      timer.cancel
      context.become(closed)
    case CancelCheckout | ExpirePayment =>
      context.become(cancelled)
  }

  def cancelled: Receive = LoggingReceive {
    case _ =>
      doNothing()
  }

  def closed: Receive = LoggingReceive {
    case _ =>
      doNothing()
  }

  private def doNothing(): Unit = {}

}
