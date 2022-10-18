package EShop.lab2

import EShop.lab2.Checkout._
import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.Await
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

}

class Checkout extends Actor {

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)
  private def scheduleTimer(finiteDuration: FiniteDuration, command: Command): Cancellable =
    scheduler.scheduleOnce(finiteDuration, self, command)(context.dispatcher, self)

  val checkoutTimerDuration = 1 seconds
  val paymentTimerDuration  = 1 seconds

  def receive: Receive = LoggingReceive {
    case StartCheckout =>
      context become selectingDelivery(scheduleTimer(checkoutTimerDuration, ExpireCheckout))
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive{
    case SelectDeliveryMethod(_) =>
      context become selectingPaymentMethod(timer)

    case CancelCheckout | ExpireCheckout =>
      timer.cancel()
      context become cancelled

  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive{
    case SelectPayment(_) =>
      context become processingPayment(scheduleTimer(paymentTimerDuration,ExpirePayment))

    case CancelCheckout | ExpireCheckout | ExpirePayment =>
      timer.cancel()
      context become cancelled
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive{
    case ConfirmPaymentReceived =>
      timer.cancel()
      context become closed
    case CancelCheckout | ExpireCheckout | ExpirePayment =>
      timer.cancel()
      context become cancelled
  }

  def closed: Receive = LoggingReceive {
    case CheckOutClosed =>
      context.stop(self)
  }

  def cancelled: Receive = LoggingReceive {
    case CancelCheckout =>
      context.stop(self)
  }

}

object CheckoutApp extends App {
  val system    = ActorSystem("Reactive")
  val mainActor = system.actorOf(Props[CartActor], "mainActor")

  mainActor ! Checkout.StartCheckout
  mainActor ! Checkout.ExpireCheckout
  mainActor ! Checkout.StartCheckout
  mainActor ! Checkout.CancelCheckout
  mainActor ! Checkout.StartCheckout
  mainActor ! Checkout.SelectDeliveryMethod
  mainActor ! Checkout.SelectPayment
  mainActor ! Checkout.ConfirmPaymentReceived

   Await.result(system.whenTerminated, Duration.Inf)
}
