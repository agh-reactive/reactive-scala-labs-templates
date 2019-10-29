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
  case object ReceivePayment                      extends Command

  sealed trait Event
  case object CheckOutClosed                   extends Event
  case class PaymentStarted(payment: ActorRef) extends Event

  def props(cart: ActorRef) = Props(new Checkout())
//  def props(cartRef: ActorRef): Props = Props(new Checkout(cartRef))

}

class Checkout extends Actor {
//  class Checkout(cartRef: ActorRef) extends Actor{

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration = 1 seconds
  val paymentTimerDuration  = 1 seconds

  def receive: Receive = LoggingReceive {
    case StartCheckout =>
      val timer = scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout)
      context become selectingDelivery(timer)
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive {
    case SelectDeliveryMethod(_) =>
      context become selectingPaymentMethod(timer)
    case CancelCheckout | ExpireCheckout => context become cancelled
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive {
    case SelectPayment(_) =>
      val timer = scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment)
      context become processingPayment(timer)
    case CancelCheckout | ExpireCheckout => context become cancelled
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive {
    case ReceivePayment                 => context become closed
    case CancelCheckout | ExpirePayment => context become cancelled
  }

  def cancelled: Receive = LoggingReceive {
    case _ => log.debug("Process was cancelled")
  }

  def closed: Receive = LoggingReceive {
    case _ => log.debug("Process was successful")
  }

//  private def cancel(timer: Cancellable): Unit = {
//    cartRef ! CartActor.CancelCheckout
//    context become cancelled
//  }
//
//  private def close(timer: Cancellable): Unit = {
//    cartRef ! CartActor.CloseCheckout
//    sender ! CheckOutClosed
//    context become closed
//  }
}
