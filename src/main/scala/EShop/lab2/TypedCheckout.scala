package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import scala.language.postfixOps

import scala.concurrent.duration._

object TypedCheckout {

  sealed trait Command
  case object StartCheckout                       extends Command
  final case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  final case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command
}

class TypedCheckout {
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
