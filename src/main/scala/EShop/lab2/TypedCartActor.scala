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

  private def scheduleTimer(context: ActorContext[TypedCartActor.Command]): Cancellable = context.scheduleOnce(cartTimerDuration, context.self, ExpireCart)

  def start: Behavior[TypedCartActor.Command] = empty

  def empty: Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case AddItem(item) =>           
          val emptyCart = Cart.empty.addItem(item)
          nonEmpty(emptyCart, scheduleTimer(context))
        case _ =>
          // val emptyCart = Cart.empty
          // nonEmpty(emptyCart, scheduleTimer(context))
          Behaviors.same
      }
  )

  def nonEmpty(cart: Cart, timer: Cancellable): Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case AddItem(item) => 
          val newCart = cart.addItem(item)
          nonEmpty(newCart, scheduleTimer(context))
        case RemoveItem(item) if cart.contains(item) =>
          val newCart = cart.removeItem(item)
          if(newCart.size == 0) {
            timer.cancel()
            empty
          }
          else nonEmpty(newCart, scheduleTimer(context));
        case StartCheckout =>
          timer.cancel()           
          inCheckout(cart)
        case ExpireCart => 
          timer.cancel()
          empty
        case _ =>
          Behaviors.same 
      }
  )

  def inCheckout(cart: Cart): Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ConfirmCheckoutCancelled =>
          nonEmpty(cart, scheduleTimer(context))
        case ConfirmCheckoutClosed => 
          empty
        case _ =>
          Behaviors.same 
      }
  )

}
