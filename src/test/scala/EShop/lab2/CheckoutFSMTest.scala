package EShop.lab2

import EShop.lab2.Checkout.{CancelCheckout, ReceivePayment, SelectDeliveryMethod, SelectPayment, StartCheckout}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}
import EShop.lab2.CheckoutFSM.Status._

import scala.concurrent.duration.{FiniteDuration, _}

class CheckoutFSMTest extends TestKit(ActorSystem("CheckoutTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
  import CheckoutFSMTest._

  it should "be in selectingDelivery state after checkout start" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
  }

  it should "be in cancelled state after cancel message received in selectingDelivery State" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! CancelCheckout
    expectMsg(cancelledMsg)
  }

  it should "be in cancelled state after expire checkout timeout in selectingDelivery state" in {
    val checkoutActor = system.actorOf(Props(new Checkout {
      override val checkoutTimerDuration: FiniteDuration = 1.seconds

      override def cancelled: Receive = {
        case any => sender ! cancelledMsg
      }
    }))

    checkoutActor ! StartCheckout
    Thread.sleep(2000)
    checkoutActor ! SelectDeliveryMethod
    expectMsg(cancelledMsg)
  }

  it should "be in selectingPayment state after delivery method selected" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod
    expectMsg(selectingPaymentMethodMsg)
  }

  it should "be in cancelled state after cancel message received in selectingPayment State" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! CancelCheckout
    expectMsg(cancelledMsg)
  }

  it should "be in cancelled state after expire checkout timeout in selectingPayment state" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod
    expectMsg(selectingPaymentMethodMsg)
    Thread.sleep(3000)
    checkoutActor ! SelectPayment
    expectNoMessage()
  }

  it should "be in processingPayment state after payment selected" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment
    expectMsg(processingPaymentMsg)
  }

  it should "be in cancelled state after cancel message received in processingPayment State" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment
    expectMsg(processingPaymentMsg)
    checkoutActor ! CancelCheckout
    expectMsg(cancelledMsg)
  }

  it should "be in cancelled state after expire checkout timeout in processingPayment state" in {
    val checkoutActor = system.actorOf(Props(new Checkout {
      override val paymentTimerDuration: FiniteDuration = 1.seconds

      override def cancelled: Receive = {
        case any => sender ! cancelledMsg
      }
    }))

    checkoutActor ! StartCheckout
    checkoutActor ! SelectDeliveryMethod
    checkoutActor ! SelectPayment
    Thread.sleep(2000)
    checkoutActor ! ReceivePayment
    expectMsg(cancelledMsg)
  }

  it should "be in closed state after payment completed" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment
    expectMsg(processingPaymentMsg)
    checkoutActor ! ReceivePayment
    expectMsg(closedMsg)
  }

  it should "not change state after cancel msg in completed state" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment
    expectMsg(processingPaymentMsg)
    checkoutActor ! ReceivePayment
    expectMsg(closedMsg)
    checkoutActor ! CancelCheckout
    expectNoMessage()
  }

}

object CheckoutFSMTest {

  val emptyMsg = "empty"
  val selectingDeliveryMsg = "selectingDelivery"
  val selectingPaymentMethodMsg = "selectingPaymentMethod"
  val processingPaymentMsg = "processingPayment"
  val cancelledMsg = "cancelled"
  val closedMsg = "closed"

  def checkoutActorWithResponseOnStateChange(system: ActorSystem) = system.actorOf(Props(new CheckoutFSM {

    onTransition {
      case NotStarted -> SelectingDelivery => {
        sender ! selectingDeliveryMsg
      }
      case SelectingDelivery -> SelectingPaymentMethod => {
        sender ! selectingPaymentMethodMsg
      }
      case SelectingPaymentMethod -> ProcessingPayment => {
        sender ! processingPaymentMsg
      }
      case ProcessingPayment -> Closed => {
        sender ! closedMsg
      }
      case ProcessingPayment -> Cancelled => {
        sender ! cancelledMsg
      }
      case SelectingPaymentMethod -> Cancelled => {
        sender ! cancelledMsg
      }
      case SelectingDelivery -> Cancelled => {
        sender ! cancelledMsg
      }
      case SelectingDelivery -> Cancelled => {
        sender ! cancelledMsg
      }
  }}))
}
