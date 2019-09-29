package EShop.lab4

import EShop.lab2.Checkout._
import akka.actor.{ActorRef, ActorSystem, Cancellable, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}

import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Random

/*
Please change tests below that persisting of state is tested. Remember that it is crucial to use the same persistenceId
for the actor to bring back his state. Use 'generatePersistenceId' to get Id, assign it to some val to use it afterwards
you terminate actor.
 */

class PersistentCheckoutTest
  extends TestKit(ActorSystem("PersistentCheckoutTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll {

  val cartActor      = TestProbe().ref
  val deliveryMethod = "post"
  val paymentMethod  = "paypal"

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)
  import PersistentCheckoutTest._

  it should "be in selectingDelivery state after checkout start" in {
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)(cartActor)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
  }

  it should "be in cancelled state after cancel message received in selectingDelivery State" in {
    val id            = ???
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)(cartActor, id)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    //restart actor
    val checkoutActorAfterRestart: ActorRef = ???
    checkoutActorAfterRestart ! CancelCheckout
    expectMsg(cancelledMsg)
  }

  it should "be in cancelled state after expire checkout timeout in selectingDelivery state" in {
    val checkoutActor = system.actorOf(Props(new PersistentCheckout(cartActor, generatePersistenceId) {
      override val timerDuration: FiniteDuration = 1.seconds

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
    val id            = ???
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)(cartActor, id)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    //restart actor
    val checkoutActorAfterRestart: ActorRef = ???
    checkoutActorAfterRestart ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
  }

  it should "be in cancelled state after cancel message received in selectingPayment State" in {
    val id            = ???
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)(cartActor, id)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
    //restart actor
    val checkoutActorAfterRestart: ActorRef = ???
    checkoutActorAfterRestart ! CancelCheckout
    expectMsg(cancelledMsg)
  }

  it should "be in cancelled state after expire checkout timeout in selectingPayment state" in {
    val checkoutActor = system.actorOf(Props(new PersistentCheckout(cartActor, generatePersistenceId) {
      override val timerDuration: FiniteDuration = 1.seconds

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
    val id            = ???
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)(cartActor, id)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
    //restart actor
    val checkoutActorAfterRestart: ActorRef = ???
    checkoutActorAfterRestart ! SelectPayment(paymentMethod)
    fishForMessage() {
      case m: String if m == processingPaymentMsg => true
      case _: PaymentStarted                      => false
    }
  }

  it should "be in cancelled state after cancel message received in processingPayment State" in {
    val id            = ???
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)(cartActor, id)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod)
    fishForMessage() {
      case m: String if m == processingPaymentMsg => true
      case _: PaymentStarted                      => false
    }
    //restart actor
    val checkoutActorAfterRestart: ActorRef = ???
    checkoutActorAfterRestart ! CancelCheckout
    expectMsg(cancelledMsg)
  }

  it should "be in cancelled state after expire checkout timeout in processingPayment state" in {
    val checkoutActor = system.actorOf(Props(new PersistentCheckout(cartActor, generatePersistenceId) {
      override val timerDuration: FiniteDuration = 1.seconds

      override def cancelled: Receive = {
        case any => sender ! cancelledMsg
      }
    }))

    checkoutActor ! StartCheckout
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    checkoutActor ! SelectPayment(paymentMethod)
    Thread.sleep(2000)
    checkoutActor ! ReceivePayment
    fishForMessage() {
      case m: String if m == cancelledMsg => true
      case _: PaymentStarted              => false
    }
  }

  it should "be in closed state after payment completed" in {
    val id            = ???
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)(cartActor, id)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod)
    fishForMessage() {
      case m: String if m == processingPaymentMsg => true
      case _: PaymentStarted                      => false
    }
    //restart actor
    val checkoutActorAfterRestart: ActorRef = ???
    checkoutActorAfterRestart ! ReceivePayment
    expectMsg(closedMsg)
  }

  it should "not change state after cancel msg in completed state" in {
    val id            = ???
    val checkoutActor = checkoutActorWithResponseOnStateChange(system)(cartActor, id)

    checkoutActor ! StartCheckout
    expectMsg(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    expectMsg(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod)
    fishForMessage() {
      case m: String if m == processingPaymentMsg => true
      case _: PaymentStarted                      => false
    }
    checkoutActor ! ReceivePayment
    expectMsg(closedMsg)
    //restart actor
    val checkoutActorAfterRestart: ActorRef = ???
    checkoutActorAfterRestart ! CancelCheckout
    expectNoMessage()
  }

}

object PersistentCheckoutTest {

  val emptyMsg                  = "empty"
  val selectingDeliveryMsg      = "selectingDelivery"
  val selectingPaymentMethodMsg = "selectingPaymentMethod"
  val processingPaymentMsg      = "processingPayment"
  val cancelledMsg              = "cancelled"
  val closedMsg                 = "closed"

  def generatePersistenceId = Random.alphanumeric.take(256).mkString

  def checkoutActorWithResponseOnStateChange(
    system: ActorSystem
  )(cartActor: ActorRef, persistenceId: String = generatePersistenceId) =
    system.actorOf(Props(new PersistentCheckout(cartActor, persistenceId) {

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
