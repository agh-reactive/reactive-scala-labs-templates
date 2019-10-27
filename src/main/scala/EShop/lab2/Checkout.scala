package EShop.lab2

import EShop.lab2.Checkout.{CancelCheckout, ExpireCheckout, ExpirePayment, ReceivePayment, SelectDeliveryMethod, SelectPayment, StartCheckout}
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object Checkout {

  sealed trait Data

  case object Uninitialized extends Data

  case class SelectingDeliveryStarted(timer: Cancellable) extends Data

  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Command

  case object StartCheckout extends Command

  case class SelectDeliveryMethod(method: String) extends Command

  case object CancelCheckout extends Command

  case object ExpireCheckout extends Command

  case class SelectPayment(payment: String) extends Command

  case object ExpirePayment extends Command

  case object ReceivePayment extends Command

  sealed trait Event

  case object CheckOutClosed extends Event

  case class PaymentStarted(payment: ActorRef) extends Event

  def props(cart: ActorRef): Props = Props(new Checkout())
}

class Checkout extends Actor {
  import Checkout._

  private val scheduler = context.system.scheduler
  private val log = Logging(context.system, this)
  implicit val executionContext: ExecutionContext = context.system.dispatcher

  val checkoutTimerDuration: FiniteDuration = 1.seconds
  val paymentTimerDuration: FiniteDuration = 1.seconds
  def checkoutTimer: Cancellable = scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout)
  def paymentTimer: Cancellable = scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment)

  def receive: Receive = LoggingReceive.withLabel("receive") {
    case StartCheckout => context.become(selectingDelivery(checkoutTimer))
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive.withLabel("selectingDelivery"){
    case SelectDeliveryMethod(_) => {
      timer.cancel()
      context.become(selectingPaymentMethod(checkoutTimer))
    }
    case ExpireCheckout | CancelCheckout => {
      timer.cancel()
      context.become(cancelled)
    }
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive.withLabel("selectingPaymentMethod") {
    case SelectPayment(_) => {
      timer.cancel()
      context.become(processingPayment(paymentTimer))
    }
    case ExpireCheckout | CancelCheckout => {
      timer.cancel()
      context.become(cancelled)
    }
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive.withLabel("processingPayment"){
    case ReceivePayment => {
      timer.cancel()
      context.become(closed)
    }
    case ExpirePayment | CancelCheckout => {
      timer.cancel()
      context.become(cancelled)
    }
  }

  def cancelled: Receive = {
    case _ => log.info("Checkout is cancelled")
  }

  def closed: Receive = {
    case _ => log.info("Checkout is closed")
  }

}
