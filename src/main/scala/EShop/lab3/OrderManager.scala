package EShop.lab3

import EShop.lab2.{TypedCartActor, TypedCheckout}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object OrderManager {

  sealed trait Command
  final case class AddItem(id: String, replyTo: ActorRef[Unit]) extends Command
  final case class RemoveItem(id: String, replyTo: ActorRef[Unit]) extends Command
  final case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String, replyTo: ActorRef[Unit]) extends Command
  final case class Buy(replyTo: ActorRef[Unit]) extends Command
  final case class Pay(replyTo: ActorRef[Unit]) extends Command
  
  final case class ConfirmCheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command]) extends Command
  final case class ConfirmPaymentStarted(paymentRef: ActorRef[Payment.Command]) extends Command
  case object ConfirmPaymentReceived extends Command

  def apply(): Behavior[Command] = start()

  private def start(): Behavior[OrderManager.Command] =
    Behaviors.setup { context =>
      val cart = context.spawn(TypedCartActor(), "cart")
      Behaviors.receiveMessage {
        case AddItem(id, replyTo) =>
          cart ! TypedCartActor.AddItem(id)
          replyTo ! ()
          Behaviors.same
        case RemoveItem(id, replyTo) =>
          cart ! TypedCartActor.RemoveItem(id)
          replyTo ! ()
          Behaviors.same
        case Buy(replyTo) =>
          cart ! TypedCartActor.StartCheckout(context.self)
          startingCheckout(cart, replyTo)
        
      }
    }

  private def startingCheckout(
    cart: ActorRef[TypedCartActor.Command],
    replyTo: ActorRef[Unit]
  ): Behavior[OrderManager.Command] =
    Behaviors.receiveMessage {
      case ConfirmCheckoutStarted(checkout) =>
        replyTo ! ()
        inCheckout(checkout)
    }

  private def inCheckout(
    checkout: ActorRef[TypedCheckout.Command]
  ): Behavior[OrderManager.Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case SelectDeliveryAndPaymentMethod(delivery, payment, replyTo) =>
          checkout ! TypedCheckout.SelectDeliveryMethod(delivery)
          checkout ! TypedCheckout.SelectPayment(payment, context.self)
          startingPayment(checkout, replyTo)
      }
    }

  private def startingPayment(
    checkout: ActorRef[TypedCheckout.Command],
    replyTo: ActorRef[Unit]
  ): Behavior[OrderManager.Command] =
    Behaviors.receiveMessage {
      case ConfirmPaymentStarted(payment) =>
        replyTo ! ()
        inPayment(checkout, payment)
    }

  private def inPayment(
    checkout: ActorRef[TypedCheckout.Command],
    payment: ActorRef[Payment.Command]
  ): Behavior[OrderManager.Command] =
    Behaviors.receiveMessage {
      case Pay(replyTo) =>
        payment ! Payment.DoPayment
        doingPayment(checkout, payment, replyTo)
    }

  private def doingPayment(
    checkout: ActorRef[TypedCheckout.Command],
    payment: ActorRef[Payment.Command],
    replyTo: ActorRef[Unit]
  ): Behavior[OrderManager.Command] =
    Behaviors.receiveMessage {
      case ConfirmPaymentReceived =>
        replyTo ! ()
        finished()
    }

  private def finished(): Behavior[OrderManager.Command] = Behaviors.stopped
}
