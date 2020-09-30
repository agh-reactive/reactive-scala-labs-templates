package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import EShop.lab3.OrderManager._
import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive

object OrderManager {

  sealed trait Command
  case class AddItem(id: String)                                               extends Command
  case class RemoveItem(id: String)                                            extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String) extends Command
  case object Buy                                                              extends Command
  case object Pay                                                              extends Command
  case class ConfirmCheckoutStarted(checkoutRef: ActorRef)                     extends Command
  case class ConfirmPaymentStarted(paymentRef: ActorRef)                       extends Command
  case object ConfirmPaymentReceived                                           extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK
}

class OrderManager extends Actor {

  override def receive = uninitialized

  def uninitialized: Receive = ???

  def open(cartActor: ActorRef): Receive = ???

  def inCheckout(cartActorRef: ActorRef, senderRef: ActorRef): Receive = {
    case CartActor.CheckoutStarted(checkoutRef, cart) => ???
  }

  def inCheckout(checkoutActorRef: ActorRef): Receive = {
    case SelectDeliveryAndPaymentMethod(delivery, payment) => ???
  }

  def inPayment(senderRef: ActorRef): Receive = {
    case Checkout.PaymentStarted(paymentRef) => ???

  }

  def inPayment(paymentActorRef: ActorRef, senderRef: ActorRef): Receive = {
    case Pay                      => ???
    case Payment.PaymentConfirmed => ???
  }

  def finished: Receive = {
    case _ => sender ! "order manager finished job"
  }
}
