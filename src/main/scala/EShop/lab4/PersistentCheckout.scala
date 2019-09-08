package EShop.lab4

import EShop.lab2.CartActor
import EShop.lab3.Payment
import akka.actor.{ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.PersistentActor

import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object PersistentCheckout {

  def props(cartActor: ActorRef, persistenceId: String) =
    Props(new PersistentCheckout(cartActor, persistenceId))
}

class PersistentCheckout(
  cartActor: ActorRef,
  val persistenceId: String
) extends PersistentActor {

  import EShop.lab2.Checkout._
  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)
  val timerDuration     = 1.seconds

  private def updateState(event: Event, maybeTimer: Option[Cancellable] = None): Unit = {
    ???
    event match {
      case CheckoutStarted                => ???
      case DeliveryMethodSelected(method) => ???
      case CheckOutClosed                 => ???
      case CheckoutCancelled              => ???
      case PaymentStarted(payment)        => ???

    }
  }

  def receiveCommand: Receive = ???

  def selectingDelivery(timer: Cancellable): Receive = ???

  def selectingPaymentMethod(timer: Cancellable): Receive = ???

  def processingPayment(timer: Cancellable): Receive = ???

  def cancelled: Receive = ???

  def closed: Receive = ???

  override def receiveRecover: Receive = ???
}
