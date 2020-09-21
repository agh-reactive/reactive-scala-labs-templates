package EShop.lab4

import EShop.lab2.{Cart, TypedCheckout}
import EShop.lab3.TypedOrderManager
import akka.actor.Cancellable
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import scala.concurrent.duration._

class TypedPersistentCartActor {

  import EShop.lab2.TypedCartActor._

  val cartTimerDuration: FiniteDuration = 5.seconds

  private def scheduleTimer(context: ActorContext[Command]): Cancellable = ???

  def apply(persistenceId: PersistenceId): Behavior[Command] = Behaviors.setup { context =>
    EventSourcedBehavior[Command, Event, State](
      persistenceId,
      Empty,
      commandHandler(context),
      eventHandler(context)
    )
  }

  def commandHandler(context: ActorContext[Command]): (State, Command) => Effect[Event, State] = (state, command) => {
    state match {
      case Empty =>
        ???

      case NonEmpty(cart, _) =>
        ???

      case InCheckout(_) =>
        ???
    }
  }

  def eventHandler(context: ActorContext[Command]): (State, Event) => State = (state, event) => {
    ???
    event match {
      case CheckoutStarted(_)        => ???
      case ItemAdded(item)           => ???
      case ItemRemoved(item)         => ???
      case CartEmptied | CartExpired => ???
      case CheckoutClosed            => ???
      case CheckoutCancelled         => ???
    }
  }

}
