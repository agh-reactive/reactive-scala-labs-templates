package EShop.lab2

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props, Timers}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {

  sealed trait Command
  case class AddItem(item: Any)        extends Command
  case class RemoveItem(item: Any)     extends Command
  case object ExpireCart               extends Command
  case object StartCheckout            extends Command
  case object ConfirmCheckoutCancelled extends Command
  case object ConfirmCheckoutClosed    extends Command

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef) extends Event

  def props = Props(new CartActor())
}

class CartActor extends Actor with Timers {
  import CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration: FiniteDuration = 5 seconds

  private def scheduleTimer: Cancellable  =
    context.system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  def receive: Receive =  LoggingReceive {
    empty
  }

  def empty: Receive = LoggingReceive{
    case AddItem(item)=>
      context become nonEmpty(Cart(List(item)), scheduleTimer)

  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive{
    case AddItem(item) =>
      timer.cancel()
      context become nonEmpty(cart.addItem(item), scheduleTimer)

    case RemoveItem(item) =>
      timer.cancel()
      if (cart.size > 1) {
        context become nonEmpty(cart.removeItem(item), scheduleTimer)
      }else if (!cart.contains(item)){
        context become nonEmpty(cart, scheduleTimer)
      } else {
        context become empty
      }

    case ExpireCart =>
      timer.cancel()
      context become empty

    case StartCheckout =>
      timer.cancel()
      context become inCheckout(cart)

  }

  def inCheckout(cart: Cart): Receive =  LoggingReceive {
    case ConfirmCheckoutCancelled =>
      context become nonEmpty(cart, scheduleTimer)
    case ConfirmCheckoutClosed =>
      context become empty
  }

}

object CartApp extends App {
  val system    = ActorSystem("Reactive")
  val mainActor = system.actorOf(Props[CartActor], "mainActor")

  mainActor ! CartActor.RemoveItem("DEF")
  mainActor ! CartActor.AddItem("ABC")
  mainActor ! CartActor.RemoveItem("DEF")
  mainActor ! CartActor.RemoveItem("ABC")
  mainActor ! CartActor.AddItem("ABC")
  mainActor ! CartActor.StartCheckout
  mainActor ! CartActor.ConfirmCheckoutClosed

  Await.result(system.whenTerminated, Duration.Inf)
}
