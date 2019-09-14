package EShop.lab2

import EShop.lab2.Checkout.{CancelCheckout, ReceivePayment, SelectDeliveryMethod, SelectPayment, StartCheckout}
import akka.actor.{ActorSystem, Cancellable, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}

import scala.concurrent.duration.{FiniteDuration, _}

class CheckoutTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll {

  val deliveryMethod = "post"
  val paymentMethod  = "paypal"

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)
  import CheckoutTest._

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
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(cancelledMsg)
  }

  it should "be in selectingPayment state after delivery method selected" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
  }

  it should "be in cancelled state after cancel message received in selectingPayment State" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! CancelCheckout
    expectMsg(cancelledMsg)
  }

  it should "be in cancelled state after expire checkout timeout in selectingPayment state" in {
    val checkoutActor = system.actorOf(Props(new Checkout {
      override val checkoutTimerDuration: FiniteDuration = 1.seconds

      override def cancelled: Receive = {
        case any => sender ! cancelledMsg
      }
    }))

    checkoutActor ! StartCheckout
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    Thread.sleep(2000)
    checkoutActor ! SelectPayment(paymentMethod)
    expectMsg(cancelledMsg)
  }

  it should "be in processingPayment state after payment selected" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod)
    expectMsg(processingPaymentMsg)
  }

  it should "be in cancelled state after cancel message received in processingPayment State" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod)
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
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    checkoutActor ! SelectPayment(paymentMethod)
    Thread.sleep(2000)
    checkoutActor ! ReceivePayment
    expectMsg(cancelledMsg)
  }

  it should "be in closed state after payment completed" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod)
    expectMsg(processingPaymentMsg)
    checkoutActor ! ReceivePayment
    expectMsg(closedMsg)
  }

  it should "not change state after cancel msg in completed state" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod)
    expectMsg(processingPaymentMsg)
    checkoutActor ! ReceivePayment
    expectMsg(closedMsg)
    checkoutActor ! CancelCheckout
    expectNoMessage()
  }

}

object CheckoutTest {

  val emptyMsg                  = "empty"
  val selectingDeliveryMsg      = "selectingDelivery"
  val selectingPaymentMethodMsg = "selectingPaymentMethod"
  val processingPaymentMsg      = "processingPayment"
  val cancelledMsg              = "cancelled"
  val closedMsg                 = "closed"

  def checkoutActorWithResponseOnStateChange(system: ActorSystem) =
    system.actorOf(Props(new Checkout {

      override def receive() = {
        val result = super.receive
        sender ! emptyMsg
        result
      }

      override def selectingDelivery(timer: Cancellable): Receive = {
        val result = super.selectingDelivery(timer)
        sender ! selectingDeliveryMsg
        result
      }

      override def selectingPaymentMethod(timer: Cancellable): Receive = {
        val result = super.selectingPaymentMethod(timer)
        sender ! selectingPaymentMethodMsg
        result
      }

      override def processingPayment(timer: Cancellable): Receive = {
        val result = super.processingPayment(timer)
        sender ! processingPaymentMsg
        result
      }

      override def cancelled: Receive = {
        val result = super.cancelled
        sender ! cancelledMsg
        result
      }

      override def closed: Receive = {
        val result = super.closed
        sender ! closedMsg
        result
      }

    }))
}
