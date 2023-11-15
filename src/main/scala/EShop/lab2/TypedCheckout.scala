package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration._
import EShop.lab3.{OrderManager, Payment}

object TypedCheckout {

  sealed trait Command
  final case class SelectDeliveryMethod(method: String) extends Command
  final case class SelectPayment(payment: String, orderManager: ActorRef[OrderManager.Command]) extends Command
  case object ConfirmPaymentReceived extends Command
  case object CancelCheckout extends Command

  private case object ExpireCheckout extends Command
  private case object ExpirePayment extends Command

  private val checkoutTimerDuration: FiniteDuration = 1.seconds
  private val paymentTimerDuration: FiniteDuration  = 1.seconds

  def apply(cart: ActorRef[TypedCartActor.Command]): Behavior[TypedCheckout.Command] =
    start(cart)

  private def start(cart: ActorRef[TypedCartActor.Command]): Behavior[TypedCheckout.Command] =
    Behaviors.setup { context =>
      selectingDelivery(cart, context.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout))
    }

  private def selectingDelivery(cart: ActorRef[TypedCartActor.Command], timer: Cancellable): Behavior[TypedCheckout.Command] =
    Behaviors.receiveMessage {
      case ExpireCheckout | CancelCheckout =>
        timer.cancel()
        cancelled(cart)
      case SelectDeliveryMethod(method) =>
        selectingPaymentMethod(cart, timer)
      case _ => Behaviors.same
    }

  private def selectingPaymentMethod(cart: ActorRef[TypedCartActor.Command], timer: Cancellable): Behavior[TypedCheckout.Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case SelectPayment(method, orderManager) =>
          timer.cancel()
          val paymentTimer = context.scheduleOnce(paymentTimerDuration, context.self, ExpirePayment)
          val payment = context.spawn(Payment(method, orderManager, context.self), "payment")
          orderManager ! OrderManager.ConfirmPaymentStarted(payment)
          processingPayment(cart, payment, paymentTimer)
        case CancelCheckout | ExpireCheckout =>
          timer.cancel()
          cancelled(cart)
        case _ => Behaviors.same
      }
    }

  private def processingPayment(
    cart: ActorRef[TypedCartActor.Command],
    payment: ActorRef[Payment.Command],
    timer: Cancellable
  ): Behavior[TypedCheckout.Command] =
    Behaviors.receiveMessage {
      case ConfirmPaymentReceived =>
        closed(cart)
      case ExpirePayment =>
        cancelled(cart)
      case _ => Behaviors.same
    }

  private def cancelled(cart: ActorRef[TypedCartActor.Command]): Behavior[TypedCheckout.Command] = {
    cart ! TypedCartActor.ConfirmCheckoutCancelled
    Behaviors.stopped
  }

  private def closed(cart: ActorRef[TypedCartActor.Command]): Behavior[TypedCheckout.Command] = {
    cart ! TypedCartActor.ConfirmCheckoutClosed
    Behaviors.stopped
  }

}
