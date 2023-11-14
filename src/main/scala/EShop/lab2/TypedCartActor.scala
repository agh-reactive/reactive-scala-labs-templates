package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.language.postfixOps
import scala.concurrent.duration._
import EShop.lab3.OrderManager

object TypedCartActor {

  sealed trait Command
  case class AddItem(item: Any)                                             extends Command
  case class RemoveItem(item: Any)                                          extends Command
  case object ExpireCart                                                    extends Command
  case class StartCheckout(orderManagerRef: ActorRef[OrderManager.Command]) extends Command
  case object ConfirmCheckoutCancelled                                      extends Command
  case object ConfirmCheckoutClosed                                         extends Command
  case class GetItems(sender: ActorRef[Cart])                               extends Command // command made to make testing easier

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command]) extends Event
}

class TypedCartActor {

  import TypedCartActor._

  val cartTimerDuration: FiniteDuration = 5 seconds

  private def scheduleTimer(context: ActorContext[Command]): Cancellable =
    context.scheduleOnce(cartTimerDuration, context.self, ExpireCart)

  def start: Behavior[Command] = empty

  def empty: Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case AddItem(item) =>
          nonEmpty(Cart(Seq(item)), scheduleTimer(context))
        case _ => Behaviors.same
      }
    }

  def nonEmpty(cart: Cart, timer: Cancellable): Behavior[Command] =
    Behaviors.receiveMessage {
      case AddItem(item) =>
        nonEmpty(cart.addItem(item), timer)
      case RemoveItem(item) if cart.contains(item) && cart.size == 1 =>
        empty
      case RemoveItem(item) if cart.contains(item) =>
        nonEmpty(cart.removeItem(item), timer)
      case RemoveItem(_) =>
        Behaviors.same
      case StartCheckout =>
        inCheckout(cart)
      case ExpireCart =>
        empty
      case _ => Behaviors.same
    }

  def inCheckout(cart: Cart): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case ConfirmCheckoutCancelled =>
          nonEmpty(cart, scheduleTimer(context))
        case ConfirmCheckoutClosed =>
          empty
        case _ => Behaviors.same
      }
    }
}
