package EShop.lab3

import akka.actor.{Actor, ActorRef, Props}

object Payment {

  sealed trait Command
  case object DoPayment extends Command

  def props(method: String, orderManager: ActorRef, checkout: ActorRef) =
    Props(new Payment(method, orderManager, checkout))
}

class Payment(
  method: String,
  orderManager: ActorRef,
  checkout: ActorRef
) extends Actor {

  override def receive: Receive = ???

}
