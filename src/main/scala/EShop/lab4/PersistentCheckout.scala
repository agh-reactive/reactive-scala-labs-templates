package EShop.lab4

import EShop.lab2.CartActor.{CloseCheckout, ItemAdded}
import EShop.lab2.{Cart, CartActor}
import EShop.lab3.Payment
import akka.actor.{ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{PersistentActor, SnapshotOffer}

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
  def scheduleTimer: Cancellable            = scheduler.scheduleOnce(timerDuration, self, Expire)
  def generateRandomId = Random.alphanumeric.take(256).mkString

  private def updateState(event: Event, maybeTimer: Option[Cancellable] = None): Unit = {
    val timer = maybeTimer.getOrElse(scheduleTimer)
    event match {
      case CheckoutStarted                => context.become(selectingDelivery(timer))
      case DeliveryMethodSelected(method) => context.become(selectingPaymentMethod(timer))
      case CheckOutClosed                 => context.become(closed)
      case CheckoutCancelled              => context.become(cancelled)
      case PaymentStarted(payment)        => context.become(processingPayment(timer))

    }
  }

  def receiveCommand: Receive = LoggingReceive.withLabel("in receive"){
    case StartCheckout => {
      persist(CheckoutStarted) { event =>
        updateState(event, Some(scheduleTimer))
      }
    }
  }

  def selectingDelivery(timer: Cancellable): Receive = {
    case SelectDeliveryMethod(method) => {
      persist(DeliveryMethodSelected(method)) {
        event => updateState(event, Some(timer))
      }
    }
    case CancelCheckout | Expire => {
      persist(CheckoutCancelled) {
        event => updateState(event, Some(timer))
      }
    }
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = {
    case SelectPayment(method) => {
      timer.cancel()
      val paymentRef = context.actorOf(Payment.props(method, sender(), self), generateRandomId)
      persist(PaymentStarted(paymentRef)) { event =>
        sender() ! event
        updateState(event, Some(scheduleTimer))
      }
    }
    case CancelCheckout | Expire => {
      persist(CheckoutCancelled) {
        event => updateState(event, Some(timer))
      }
    }
  }

  def processingPayment(timer: Cancellable): Receive = {
    case ReceivePayment => {
      timer.cancel()
      cartActor ! CartActor.CloseCheckout
      persist(CheckOutClosed) { event =>
        updateState(event, Some(scheduleTimer))
      }
    }
    case CancelCheckout | Expire => {
      persist(CheckoutCancelled) {
        event => updateState(event, Some(timer))
      }
    }
  }

  def cancelled: Receive = {
    case _ => log.info("Checkout is cancelled")
  }

  def closed: Receive = {
    case _ => log.info("Checkout is closed")
  }

  override def receiveRecover: Receive = {
    case event: Event     => updateState(event)
    case _: SnapshotOffer => log.error("Received unhandled snapshot offer")
  }
}
