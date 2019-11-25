package EShop.lab4

import EShop.lab2.CartActor
import EShop.lab3.Payment
import akka.actor.{ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.concurrent.duration._
import scala.util.Random

object PersistentCheckout {

  def props(cartActor: ActorRef, persistenceId: String) =
    Props(new PersistentCheckout(cartActor, persistenceId))
}

class PersistentCheckout(
  cartActor: ActorRef,
  val persistenceId: String
) extends PersistentActor {
  import EShop.lab2.Checkout._
  import context._

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)
  val timerDuration     = 1.seconds

  private def updateState(event: Event, maybeTimer: Option[Cancellable] = None): Unit = {
    val timer = maybeTimer.getOrElse(schedule)
    event match {
      case CheckoutStarted           => become(selectingDelivery(timer))
      case DeliveryMethodSelected(_) => become(selectingPaymentMethod(timer))
      case CheckOutClosed            => become(closed)
      case CheckoutCancelled         => become(cancelled)
      case PaymentStarted(_)         => become(processingPayment(timer))

    }
  }

  def receiveCommand: Receive = LoggingReceive {
    case StartCheckout =>
      val timer = scheduler.scheduleOnce(delay = timerDuration, receiver = self, message = Expire)
      persist(CheckoutStarted)(event => updateState(event, Some(timer)))
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive {
    case SelectDeliveryMethod(method) =>
      persist(DeliveryMethodSelected(method))(event => updateState(event, Some(timer)))
    case CancelCheckout | Expire => persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive {
    case SelectPayment(paymentMethod) =>
      timer.cancel()
      val paymentTimer =
        scheduler.scheduleOnce(delay = timerDuration, receiver = self, message = Expire)(
          context.system.dispatcher
        )
      val paymentService = {
        context.actorOf(Payment.props(paymentMethod, sender(), self), Random.alphanumeric.take(256).mkString)
      }
      val event = PaymentStarted(paymentService)
      persist(event) { savedEvent =>
        sender() ! savedEvent
        updateState(savedEvent, Some(paymentTimer))
      }
    case CancelCheckout | Expire => persist(CheckoutCancelled)(event => updateState(event, Some(timer)))
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive {
    case ReceivePayment =>
      timer.cancel()
      cartActor ! CartActor.CloseCheckout
      persist(CheckOutClosed)(event => updateState(event))
    case CancelCheckout | Expire => persist(CheckoutCancelled)(event => updateState(event))
  }

  def cancelled: Receive = {
    case e => log.debug("Cancelled", e)
  }

  def closed: Receive = {
    case e => log.debug("Closed", e)
  }

  override def receiveRecover: Receive = {
    case event: Event     => updateState(event)
    case _: SnapshotOffer => log.error("Received unhandled snapshot offer")
  }

  private def schedule = scheduler.scheduleOnce(delay = timerDuration, receiver = self, message = Expire)

}
