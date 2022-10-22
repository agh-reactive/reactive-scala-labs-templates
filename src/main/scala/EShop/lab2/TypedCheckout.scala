package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import scala.language.postfixOps

import scala.concurrent.duration._

object TypedCheckout {

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
  case object CheckOutClosed                        extends Event
  case class PaymentStarted(payment: ActorRef[Any]) extends Event
}

class TypedCheckout {
  import TypedCheckout._

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  def start: Behavior[TypedCheckout.Command] = Behaviors.receive { (context, message) =>
    message match {
      case StartCheckout =>
        selectingDelivery(
          context.system.scheduler.scheduleOnce(checkoutTimerDuration,
            () => context.self ! ExpireCheckout)(context.system.executionContext)
        )
    }
  }

  def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive { (context, message) =>
    message match {
      case SelectDeliveryMethod(_) =>
        selectingPaymentMethod(timer)
      case CancelCheckout =>
        timer.cancel()
        cancelled
      case ExpireCheckout =>
        cancelled
    }
  }

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive { (context, message) =>
    message match {
      case SelectPayment(_) =>
        timer.cancel()
        processingPayment(
          context.system.scheduler.scheduleOnce(paymentTimerDuration,
            () => context.self ! ExpirePayment)(context.system.executionContext)
        )
      case CancelCheckout =>
        timer.cancel()
        cancelled
      case ExpireCheckout =>
        cancelled
    }
  }

  def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive { (_, message) =>
    message match {
      case ConfirmPaymentReceived =>
        timer.cancel()
        closed
      case CancelCheckout =>
        cancelled
      case ExpirePayment =>
        cancelled
    }
  }

  def cancelled: Behavior[TypedCheckout.Command] = Behaviors.stopped

  def closed: Behavior[TypedCheckout.Command] = Behaviors.stopped

}
