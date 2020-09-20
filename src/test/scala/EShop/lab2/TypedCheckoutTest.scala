package EShop.lab2

import EShop.lab3.TypedOrderManager
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.Cancellable
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration.{FiniteDuration, _}

class TypedCheckoutTest extends ScalaTestWithActorTestKit with AnyFlatSpecLike with BeforeAndAfterAll {

  val deliveryMethod = "post"
  val paymentMethod  = "paypal"

  override def afterAll: Unit = testKit.shutdownTestKit()

  import TypedCheckoutTest._
  import TypedCheckout._

  it should "be in selectingDelivery state after checkout start" in {
    val probe          = testKit.createTestProbe[String]
    val cartActorProbe = testKit.createTestProbe[TypedCartActor.Command]
    val checkoutActor  = checkoutActorWithResponseOnStateChange(testKit, probe.ref, cartActorProbe.ref)

    probe.expectMessage(emptyMsg)
    checkoutActor ! StartCheckout
    probe.expectMessage(selectingDeliveryMsg)
  }

  it should "be in cancelled state after cancel message received in selectingDelivery State" in {
    val probe          = testKit.createTestProbe[String]
    val cartActorProbe = testKit.createTestProbe[TypedCartActor.Command]
    val checkoutActor  = checkoutActorWithResponseOnStateChange(testKit, probe.ref, cartActorProbe.ref)

    probe.expectMessage(emptyMsg)
    checkoutActor ! StartCheckout
    probe.expectMessage(selectingDeliveryMsg)
    checkoutActor ! CancelCheckout
    probe.expectMessage(cancelledMsg)
  }

  it should "be in cancelled state after expire checkout timeout in selectingDelivery state" in {
    val probe = testKit.createTestProbe[String]
    val checkoutActor = testKit.spawn {
      val checkout = new TypedCheckout(testKit.createTestProbe[TypedCartActor.Command]().ref) {
        override val checkoutTimerDuration: FiniteDuration = 1.seconds

        override def cancelled: Behavior[TypedCheckout.Command] =
          Behaviors.receiveMessage({ _ =>
            probe.ref ! cancelledMsg
            Behaviors.same
          })
      }
      checkout.start
    }

    checkoutActor ! StartCheckout
    Thread.sleep(2000)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    probe.expectMessage(cancelledMsg)
  }

  it should "be in selectingPayment state after delivery method selected" in {
    val probe          = testKit.createTestProbe[String]
    val cartActorProbe = testKit.createTestProbe[TypedCartActor.Command]
    val checkoutActor  = checkoutActorWithResponseOnStateChange(testKit, probe.ref, cartActorProbe.ref)

    probe.expectMessage(emptyMsg)
    checkoutActor ! StartCheckout
    probe.expectMessage(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    probe.expectMessage(selectingPaymentMethodMsg)
  }

  it should "be in cancelled state after cancel message received in selectingPayment State" in {
    val probe          = testKit.createTestProbe[String]
    val cartActorProbe = testKit.createTestProbe[TypedCartActor.Command]
    val checkoutActor  = checkoutActorWithResponseOnStateChange(testKit, probe.ref, cartActorProbe.ref)

    probe.expectMessage(emptyMsg)
    checkoutActor ! StartCheckout
    probe.expectMessage(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    probe.expectMessage(selectingPaymentMethodMsg)
    checkoutActor ! CancelCheckout
    probe.expectMessage(cancelledMsg)
  }

  it should "be in cancelled state after expire checkout timeout in selectingPayment state" in {
    val probe             = testKit.createTestProbe[String]
    val orderManagerProbe = testKit.createTestProbe[TypedOrderManager.Command]
    val checkoutActor = testKit.spawn {
      val checkout = new TypedCheckout(testKit.createTestProbe[TypedCartActor.Command]().ref) {
        override val checkoutTimerDuration: FiniteDuration = 1.seconds

        override def cancelled: Behavior[TypedCheckout.Command] =
          Behaviors.receiveMessage({ _ =>
            probe.ref ! cancelledMsg
            Behaviors.same
          })
      }
      checkout.start
    }

    checkoutActor ! StartCheckout
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    Thread.sleep(2000)
    checkoutActor ! SelectPayment(paymentMethod, orderManagerProbe.ref)
    probe.expectMessage(cancelledMsg)
  }

  it should "be in processingPayment state after payment selected" in {
    val probe             = testKit.createTestProbe[String]
    val cartActorProbe    = testKit.createTestProbe[TypedCartActor.Command]
    val orderManagerProbe = testKit.createTestProbe[TypedOrderManager.Command]
    val checkoutActor     = checkoutActorWithResponseOnStateChange(testKit, probe.ref, cartActorProbe.ref)

    probe.expectMessage(emptyMsg)
    checkoutActor ! StartCheckout
    probe.expectMessage(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    probe.expectMessage(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod, orderManagerProbe.ref)
    probe.expectMessage(processingPaymentMsg)
  }

  it should "be in cancelled state after cancel message received in processingPayment State" in {
    val probe             = testKit.createTestProbe[String]
    val cartActorProbe    = testKit.createTestProbe[TypedCartActor.Command]
    val orderManagerProbe = testKit.createTestProbe[TypedOrderManager.Command]
    val checkoutActor     = checkoutActorWithResponseOnStateChange(testKit, probe.ref, cartActorProbe.ref)

    probe.expectMessage(emptyMsg)
    checkoutActor ! StartCheckout
    probe.expectMessage(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    probe.expectMessage(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod, orderManagerProbe.ref)
    probe.expectMessage(processingPaymentMsg)
    checkoutActor ! CancelCheckout
    probe.expectMessage(cancelledMsg)
  }

  it should "be in cancelled state after expire checkout timeout in processingPayment state" in {
    val probe             = testKit.createTestProbe[String]
    val orderManagerProbe = testKit.createTestProbe[TypedOrderManager.Command]
    val checkoutActor = testKit.spawn {
      val checkout = new TypedCheckout(testKit.createTestProbe[TypedCartActor.Command]().ref) {
        override val paymentTimerDuration: FiniteDuration = 1.seconds

        override def cancelled: Behavior[TypedCheckout.Command] =
          Behaviors.receiveMessage({ _ =>
            probe.ref ! cancelledMsg
            Behaviors.same
          })
      }
      checkout.start
    }

    checkoutActor ! StartCheckout
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    checkoutActor ! SelectPayment(paymentMethod, orderManagerProbe.ref)
    Thread.sleep(2000)
    checkoutActor ! ConfirmPaymentReceived
    probe.expectMessage(cancelledMsg)
  }

  it should "be in closed state after payment completed" in {
    val probe             = testKit.createTestProbe[String]()
    val cartActorProbe    = testKit.createTestProbe[TypedCartActor.Command]
    val orderManagerProbe = testKit.createTestProbe[TypedOrderManager.Command]
    val checkoutActor     = checkoutActorWithResponseOnStateChange(testKit, probe.ref, cartActorProbe.ref)

    probe.expectMessage(emptyMsg)
    checkoutActor ! StartCheckout
    probe.expectMessage(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    probe.expectMessage(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod, orderManagerProbe.ref)
    probe.expectMessage(processingPaymentMsg)
    checkoutActor ! ConfirmPaymentReceived
    probe.expectMessage(closedMsg)
  }

  it should "not change state after cancel msg in completed state" in {
    val probe             = testKit.createTestProbe[String]()
    val cartActorProbe    = testKit.createTestProbe[TypedCartActor.Command]
    val orderManagerProbe = testKit.createTestProbe[TypedOrderManager.Command]
    val checkoutActor     = checkoutActorWithResponseOnStateChange(testKit, probe.ref, cartActorProbe.ref)

    probe.expectMessage(emptyMsg)
    checkoutActor ! StartCheckout
    probe.expectMessage(selectingDeliveryMsg)
    checkoutActor ! SelectDeliveryMethod(deliveryMethod)
    probe.expectMessage(selectingPaymentMethodMsg)
    checkoutActor ! SelectPayment(paymentMethod, orderManagerProbe.ref)
    probe.expectMessage(processingPaymentMsg)
    checkoutActor ! ConfirmPaymentReceived
    probe.expectMessage(closedMsg)
    checkoutActor ! CancelCheckout
    probe.expectNoMessage()
  }

}

object TypedCheckoutTest {

  val emptyMsg                  = "empty"
  val selectingDeliveryMsg      = "selectingDelivery"
  val selectingPaymentMethodMsg = "selectingPaymentMethod"
  val processingPaymentMsg      = "processingPayment"
  val cancelledMsg              = "cancelled"
  val closedMsg                 = "closed"

  def checkoutActorWithResponseOnStateChange(
    testkit: ActorTestKit,
    probe: ActorRef[String],
    cartActorProbe: ActorRef[TypedCartActor.Command]
  ): ActorRef[TypedCheckout.Command] =
    testkit.spawn {
      val checkout = new TypedCheckout(cartActorProbe) {

        override def start: Behavior[TypedCheckout.Command] =
          Behaviors.setup(_ => {
            probe ! emptyMsg
            super.start
          })

        override def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] =
          Behaviors.setup(_ => {
            val result = super.selectingDelivery(timer)
            probe ! selectingDeliveryMsg
            result
          })

        override def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] =
          Behaviors.setup(_ => {
            probe ! selectingPaymentMethodMsg
            super.selectingPaymentMethod(timer)
          })

        override def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] =
          Behaviors.setup(_ => {
            probe ! processingPaymentMsg
            super.processingPayment(timer)
          })

        override def cancelled: Behavior[TypedCheckout.Command] =
          Behaviors.setup(_ => {
            probe ! cancelledMsg
            super.cancelled
          })

        override def closed: Behavior[TypedCheckout.Command] =
          Behaviors.setup(_ => {
            probe ! closedMsg
            super.closed
          })
      }
      checkout.start
    }
}
