package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import EShop.lab3.OrderManager._
import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive

object OrderManager {
  sealed trait State
  case object Uninitialized extends State
  case object Open          extends State
  case object InCheckout    extends State
  case object InPayment     extends State
  case object Finished      extends State

  sealed trait Command
  case class AddItem(id: String)                                               extends Command
  case class RemoveItem(id: String)                                            extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String) extends Command
  case object Buy                                                              extends Command
  case object Pay                                                              extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK

  sealed trait Data
  case object Empty                                                            extends Data
  case class CartData(cartRef: ActorRef)                                       extends Data
  case class CartDataWithSender(cartRef: ActorRef, sender: ActorRef)           extends Data
  case class InCheckoutData(checkoutRef: ActorRef)                             extends Data
  case class InCheckoutDataWithSender(checkoutRef: ActorRef, sender: ActorRef) extends Data
  case class InPaymentData(paymentRef: ActorRef)                               extends Data
  case class InPaymentDataWithSender(paymentRef: ActorRef, sender: ActorRef)   extends Data
}

class OrderManager extends Actor {

  override def receive = uninitialized

  def uninitialized: Receive = ???

  def open(cartActor: ActorRef): Receive = ???

  def inCheckout(cartActorRef: ActorRef, senderRef: ActorRef): Receive = {
    case CartActor.CheckoutStarted(checkoutRef) => ???
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
