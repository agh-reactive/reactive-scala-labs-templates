package EShop.lab2

import EShop.lab2.Cart._
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object Cart {

  case class Storage(items: Seq[Any]) {
    def contains(item: Any): Boolean   = ???
    def addItem(item: Any): Storage    = ???
    def removeItem(item: Any): Storage = ???
    def size: Int                      = ???
  }

  object Storage {
    def empty: Storage = ???
  }

  sealed trait Command

  case class AddItem(item: Any) extends Command

  case class RemoveItem(item: Any) extends Command

  case object ExpireCart extends Command

  case object StartCheckout extends Command

  case object CancelCheckout extends Command

  case object CloseCheckout extends Command

  sealed trait Event

  case class CheckoutStarted(checkoutRef: ActorRef) extends Event

  def props = Props(new Cart)
}

class Cart extends Actor {

  private val log = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer: Cancellable = ???

  def receive: Receive = empty

  def empty: Receive = ???

  def nonEmpty(cart: Storage, timer: Cancellable): Receive = ???

  def inCheckout(cart: Storage): Receive = ???

}
