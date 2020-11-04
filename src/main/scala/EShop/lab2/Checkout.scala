package EShop.lab2

import EShop.lab2.CartActor.ConfirmCheckoutClosed
import EShop.lab2.Checkout._
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent._
import ExecutionContext.Implicits.global
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

  def props(cart: ActorRef) = Props(new Checkout(cart))
}

class Checkout(
  cartActor: ActorRef
) extends Actor {

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)
  val checkoutTimerDuration = 1 seconds
  val paymentTimerDuration  = 1 seconds

  def receive: Receive = LoggingReceive{
    case StartCheckout => context become selectingDelivery(scheduler.scheduleOnce(checkoutTimerDuration) {
      self ! ExpireCheckout
    })
    case CancelCheckout =>
      context become cancelled
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive{
    case SelectDeliveryMethod(method) =>
      log.info(s"Selected $method delivery method")
      context become selectingPaymentMethod(timer)
    case CancelCheckout =>
      log.info(s"Checkout has been cancelled")
      context become cancelled
    case ExpireCheckout =>
      log.info(s"Checkout time has expired")
      context become cancelled
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive{
    case SelectPayment(payment) =>
      log.info(s"Selected $payment payment method")
      context become processingPayment(scheduler.scheduleOnce(paymentTimerDuration){
        self ! ExpirePayment
      })
    case ExpirePayment =>
      context become cancelled

    case CancelCheckout =>
      context become cancelled

    case ExpireCheckout =>
      context become cancelled
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive{
    case ConfirmPaymentReceived =>
      timer.cancel()
      sender ! ConfirmCheckoutClosed
      context become closed
    case CancelCheckout =>
      context become cancelled
    case ExpirePayment =>
      context become cancelled
    case ExpireCheckout =>
      context become cancelled
  }

  def cancelled: Receive = LoggingReceive{
    case _ =>
      log.info(s"Checkout is cancelled")
      context.stop(self)
  }

  def closed: Receive = LoggingReceive {
    case _ => context.stop(self)
  }

}
