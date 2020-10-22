package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.language.postfixOps
import scala.concurrent.duration._

object TypedCartActor {

  sealed trait Command
  case class AddItem(item: Any)        extends Command
  case class RemoveItem(item: Any)     extends Command
  case object ExpireCart               extends Command
  case object StartCheckout            extends Command
  case object ConfirmCheckoutCancelled extends Command
  case object ConfirmCheckoutClosed    extends Command

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command]) extends Event
}

class TypedCartActor {

  import TypedCartActor._

  val cartTimerDuration: FiniteDuration = 5 seconds

  private def scheduleTimer(context: ActorContext[TypedCartActor.Command]): Cancellable =
    context.scheduleOnce(cartTimerDuration, context.self, ExpireCart)

  def start: Behavior[TypedCartActor.Command] = empty

  def empty: Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case AddItem(item) => nonEmpty(Cart.empty.addItem(item), scheduleTimer(context))
        case _             => Behaviors.same
    }
  )

  def nonEmpty(cart: Cart, timer: Cancellable): Behavior[TypedCartActor.Command] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case AddItem(item) => nonEmpty(cart.addItem(item), timer)
          case RemoveItem(item) if cart.contains(item) =>
            if (cart.size != 1) nonEmpty(cart.removeItem(item), timer) else empty
          case StartCheckout => inCheckout(cart)
          case ExpireCart    => empty
      }
    )

  def inCheckout(cart: Cart): Behavior[TypedCartActor.Command] =
    Behaviors.receive(
      (context, msg) =>
        msg match {
          case ConfirmCheckoutCancelled => nonEmpty(cart, scheduleTimer(context))
          case ConfirmCheckoutClosed    => empty
      }
    )

}
