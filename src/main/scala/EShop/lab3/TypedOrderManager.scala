package EShop.lab3

import EShop.lab2.{TypedCartActor, TypedCheckout}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object TypedOrderManager {

  sealed trait Command
  case class AddItem(id: String, sender: ActorRef[Ack])                                               extends Command
  case class RemoveItem(id: String, sender: ActorRef[Ack])                                            extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String, sender: ActorRef[Ack]) extends Command
  case class Buy(sender: ActorRef[Ack])                                                               extends Command
  case class Pay(sender: ActorRef[Ack])                                                               extends Command
  case class ConfirmCheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command])                     extends Command
  case class ConfirmPaymentStarted(paymentRef: ActorRef[TypedPayment.Command])                        extends Command
  case object ConfirmPaymentReceived                                                                  extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK
}

class TypedOrderManager {

  import TypedOrderManager._

  def start: Behavior[TypedOrderManager.Command] = ???

  def uninitialized: Behavior[TypedOrderManager.Command] = ???

  def open(cartActor: ActorRef[TypedCartActor.Command]): Behavior[TypedOrderManager.Command] = ???

  def inCheckout(
    cartActorRef: ActorRef[TypedCartActor.Command],
    senderRef: ActorRef[Ack]
  ): Behavior[TypedOrderManager.Command] = ???

  def inCheckout(checkoutActorRef: ActorRef[TypedCheckout.Command]): Behavior[TypedOrderManager.Command] = ???

  def inPayment(senderRef: ActorRef[Ack]): Behavior[TypedOrderManager.Command] = ???

  def inPayment(
    paymentActorRef: ActorRef[TypedPayment.Command],
    senderRef: ActorRef[Ack]
  ): Behavior[TypedOrderManager.Command] = ???

  def finished: Behavior[TypedOrderManager.Command] = ???
}
