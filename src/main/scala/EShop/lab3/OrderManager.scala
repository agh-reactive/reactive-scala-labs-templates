package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import EShop.lab3.OrderManager._
import EShop.lab3.Payment.DoPayment
import akka.actor.{Actor, ActorRef}
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

  def uninitialized: Receive = LoggingReceive{
    case AddItem(item) =>
      val cartRef: ActorRef = context.system.actorOf(CartActor.props())
      cartRef ! CartActor.AddItem(item)
      sender ! Done
      context become open(cartRef)
  }

  def open(cartActor: ActorRef): Receive = LoggingReceive{
    case AddItem(item) =>
      cartActor ! CartActor.AddItem(item)
      sender ! Done

    case RemoveItem(item) =>
      cartActor ! CartActor.RemoveItem(item)
      sender ! Done

    case Buy =>
      cartActor ! CartActor.StartCheckout
      context become inCheckout(cartActor, sender)
  }

  def inCheckout(cartActorRef: ActorRef, senderRef: ActorRef): Receive =    LoggingReceive {
    case CartActor.CheckoutStarted(checkoutRef) =>
      cartActorRef ! Done
      senderRef ! Done
      context become inCheckout(checkoutRef)


  def inCheckout(checkoutActorRef: ActorRef): Receive = LoggingReceive{
    case SelectDeliveryAndPaymentMethod(delivery, payment) =>
      checkoutActorRef ! Checkout.SelectDeliveryMethod(delivery)
      checkoutActorRef ! Checkout.SelectPayment(payment)
      context become inPayment(sender)}
  }

  def inPayment(senderRef: ActorRef): Receive = LoggingReceive {
    case Checkout.PaymentStarted(paymentRef) =>
      senderRef ! Done
      context become inPayment(paymentRef, senderRef)
  }

  def inPayment(paymentActorRef: ActorRef, senderRef: ActorRef): Receive = LoggingReceive{
    case Pay =>
      paymentActorRef ! DoPayment
      senderRef ! Done

    case ConfirmPaymentReceived =>
      senderRef ! Done
      context become finished
  }

  def finished: Receive = {
    case _ => sender ! "order manager finished job"
  }
}
