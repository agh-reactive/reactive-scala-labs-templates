package EShop.lab5

import EShop.lab3.Payment.DoPayment
import EShop.lab5.Payment.{PaymentRejected, PaymentRestarted}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}

import scala.concurrent.duration._

object Payment {

  case object PaymentRejected
  case object PaymentRestarted

  def props(method: String, orderManager: ActorRef, checkout: ActorRef) =
    Props(new Payment(method, orderManager, checkout))

}

class Payment(
  method: String,
  orderManager: ActorRef,
  checkout: ActorRef
) extends Actor
  with ActorLogging {

  override def receive: Receive = {
    case DoPayment => // use payment service
  }

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1.seconds) {
      ???
    }

  //please use this one to notify when supervised actor was stoped
  private def notifyAboutRejection(): Unit = {
    orderManager ! PaymentRejected
    checkout ! PaymentRejected
  }

  //please use this one to notify when supervised actor was restarted
  private def notifyAboutRestart(): Unit = {
    orderManager ! PaymentRestarted
    checkout ! PaymentRestarted
  }
}
