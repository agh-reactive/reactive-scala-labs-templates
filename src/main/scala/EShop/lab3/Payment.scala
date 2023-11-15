package EShop.lab3

import EShop.lab2.TypedCheckout
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object Payment {

  sealed trait Command
  case object DoPayment extends Command

  def apply(
    method: String,
    orderManager: ActorRef[OrderManager.Command],
    checkout: ActorRef[TypedCheckout.Command]
  ): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case DoPayment =>
          orderManager ! OrderManager.ConfirmPaymentReceived
          checkout ! TypedCheckout.ConfirmPaymentReceived
          Behaviors.same
      }
    }

}
