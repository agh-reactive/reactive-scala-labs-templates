package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import scala.language.postfixOps

import scala.concurrent.duration._
import EShop.lab3.OrderManager

object TypedCheckout {

  sealed trait Command
  case object StartCheckout                                                                  extends Command
  case class SelectDeliveryMethod(method: String)                                            extends Command
  case object CancelCheckout                                                                 extends Command
  case object ExpireCheckout                                                                 extends Command
  case class SelectPayment(payment: String, orderManagerRef: ActorRef[OrderManager.Command]) extends Command
  case object ExpirePayment                                                                  extends Command
  case object ConfirmPaymentReceived                                                         extends Command

  sealed trait Event
  case object CheckOutClosed                           extends Event
  case class PaymentStarted(paymentRef: ActorRef[Any]) extends Event
}

class TypedCheckout(
  cartActor: ActorRef[TypedCartActor.Command]
) {
  import TypedCheckout._

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  def start: Behavior[TypedCheckout.Command] =
    Behaviors.setup { context =>
      selectingDelivery(context.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout))
    }

  def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] =
    Behaviors.receiveMessage {
      case ExpireCheckout | CancelCheckout =>
        cancelled
      case SelectDeliveryMethod(method) =>
        selectingPaymentMethod(timer)
      case _ => Behaviors.same
    }

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] =
    Behaviors.receiveMessage {
      case SelectPayment(payment) =>
        processingPayment(timer)
      case CancelCheckout | ExpireCheckout =>
        cancelled
      case _ => Behaviors.same
    }

  def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] =
    Behaviors.receiveMessage {
      case ConfirmPaymentReceived =>
        closed
      case CancelCheckout | ExpireCheckout =>
        cancelled
      case _ => Behaviors.same
    }

  def cancelled: Behavior[TypedCheckout.Command] =
    Behaviors.stopped

  def closed: Behavior[TypedCheckout.Command] =
    Behaviors.stopped

}
