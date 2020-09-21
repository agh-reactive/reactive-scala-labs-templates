package EShop.lab4

import EShop.lab2.TypedCartActor
import EShop.lab3.TypedOrderManager
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings
import akka.persistence.typed.PersistenceId
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.flatspec.AnyFlatSpecLike

import scala.concurrent.duration._
import scala.util.Random

class TypedPersistentCheckoutTest
  extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
  with AnyFlatSpecLike
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  override def afterAll: Unit = testKit.shutdownTestKit()

  import EShop.lab2.TypedCheckout._

  private val cartActorProbe = testKit.createTestProbe[TypedCartActor.Command]()

  private val orderManagerProbe = testKit.createTestProbe[TypedOrderManager.Command]

  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[Command, Event, State](
      system,
      new TypedPersistentCheckout {
        override val timerDuration: FiniteDuration = 1.second
      }.apply(cartActorProbe.ref, generatePersistenceId),
      SerializationSettings.disabled
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  val deliveryMethod = "post"
  val paymentMethod  = "paypal"

  def generatePersistenceId: PersistenceId = PersistenceId.ofUniqueId(Random.alphanumeric.take(256).mkString)

  it should "be in selectingDelivery state after checkout start" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true
  }

  it should "be in cancelled state after cancel message received in selectingDelivery State" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true

    val resultCancelCheckout = eventSourcedTestKit.runCommand(CancelCheckout)

    resultCancelCheckout.event shouldBe CheckoutCancelled
    resultCancelCheckout.state shouldBe Cancelled
  }

  it should "be in cancelled state after expire checkout timeout in selectingDelivery state" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true

    Thread.sleep(2000)

    val resultSelectDelivery = eventSourcedTestKit.runCommand(SelectDeliveryMethod(deliveryMethod))

    resultSelectDelivery.hasNoEvents shouldBe true
    resultSelectDelivery.state shouldBe Cancelled
  }

  it should "be in selectingPayment state after delivery method selected" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true

    val resultSelectDelivery = eventSourcedTestKit.runCommand(SelectDeliveryMethod(deliveryMethod))

    resultSelectDelivery.event.isInstanceOf[DeliveryMethodSelected] shouldBe true
    resultSelectDelivery.state.isInstanceOf[SelectingPaymentMethod] shouldBe true
  }

  it should "be in cancelled state after cancel message received in selectingPayment State" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true

    val resultSelectDelivery = eventSourcedTestKit.runCommand(SelectDeliveryMethod(deliveryMethod))

    resultSelectDelivery.event.isInstanceOf[DeliveryMethodSelected] shouldBe true
    resultSelectDelivery.state.isInstanceOf[SelectingPaymentMethod] shouldBe true

    val resultCancelCheckout = eventSourcedTestKit.runCommand(CancelCheckout)

    resultCancelCheckout.event shouldBe CheckoutCancelled
    resultCancelCheckout.state shouldBe Cancelled
  }

  it should "be in cancelled state after expire checkout timeout in selectingPayment state" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true

    val resultSelectDelivery = eventSourcedTestKit.runCommand(SelectDeliveryMethod(deliveryMethod))

    resultSelectDelivery.event.isInstanceOf[DeliveryMethodSelected] shouldBe true
    resultSelectDelivery.state.isInstanceOf[SelectingPaymentMethod] shouldBe true

    Thread.sleep(2000)

    val resultSelectPayment = eventSourcedTestKit.runCommand(SelectPayment(paymentMethod, orderManagerProbe.ref))

    resultSelectPayment.hasNoEvents shouldBe true
    resultSelectPayment.state shouldBe Cancelled
  }

  it should "be in processingPayment state after payment selected" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true

    val resultSelectDelivery = eventSourcedTestKit.runCommand(SelectDeliveryMethod(deliveryMethod))

    resultSelectDelivery.event.isInstanceOf[DeliveryMethodSelected] shouldBe true
    resultSelectDelivery.state.isInstanceOf[SelectingPaymentMethod] shouldBe true

    val resultSelectPayment = eventSourcedTestKit.runCommand(SelectPayment(paymentMethod, orderManagerProbe.ref))

    resultSelectPayment.event.isInstanceOf[PaymentStarted] shouldBe true
    resultSelectPayment.state.isInstanceOf[ProcessingPayment] shouldBe true
  }

  it should "be in cancelled state after cancel message received in processingPayment State" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true

    val resultSelectDelivery = eventSourcedTestKit.runCommand(SelectDeliveryMethod(deliveryMethod))

    resultSelectDelivery.event.isInstanceOf[DeliveryMethodSelected] shouldBe true
    resultSelectDelivery.state.isInstanceOf[SelectingPaymentMethod] shouldBe true

    val resultSelectPayment = eventSourcedTestKit.runCommand(SelectPayment(paymentMethod, orderManagerProbe.ref))

    resultSelectPayment.event.isInstanceOf[PaymentStarted] shouldBe true
    resultSelectPayment.state.isInstanceOf[ProcessingPayment] shouldBe true

    val resultCancelCheckout = eventSourcedTestKit.runCommand(CancelCheckout)

    resultCancelCheckout.event shouldBe CheckoutCancelled
    resultCancelCheckout.state shouldBe Cancelled
  }

  it should "be in cancelled state after expire checkout timeout in processingPayment state" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true

    val resultSelectDelivery = eventSourcedTestKit.runCommand(SelectDeliveryMethod(deliveryMethod))

    resultSelectDelivery.event.isInstanceOf[DeliveryMethodSelected] shouldBe true
    resultSelectDelivery.state.isInstanceOf[SelectingPaymentMethod] shouldBe true

    val resultSelectPayment = eventSourcedTestKit.runCommand(SelectPayment(paymentMethod, orderManagerProbe.ref))

    resultSelectPayment.event.isInstanceOf[PaymentStarted] shouldBe true
    resultSelectPayment.state.isInstanceOf[ProcessingPayment] shouldBe true

    Thread.sleep(2000)

    val resultReceivePayment = eventSourcedTestKit.runCommand(ConfirmPaymentReceived)

    resultReceivePayment.hasNoEvents shouldBe true
    resultReceivePayment.state shouldBe Cancelled
  }

  it should "be in closed state after payment completed" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true

    val resultSelectDelivery = eventSourcedTestKit.runCommand(SelectDeliveryMethod(deliveryMethod))

    resultSelectDelivery.event.isInstanceOf[DeliveryMethodSelected] shouldBe true
    resultSelectDelivery.state.isInstanceOf[SelectingPaymentMethod] shouldBe true

    val resultSelectPayment = eventSourcedTestKit.runCommand(SelectPayment(paymentMethod, orderManagerProbe.ref))

    resultSelectPayment.event.isInstanceOf[PaymentStarted] shouldBe true
    resultSelectPayment.state.isInstanceOf[ProcessingPayment] shouldBe true

    val resultReceivePayment = eventSourcedTestKit.runCommand(ConfirmPaymentReceived)

    resultReceivePayment.event shouldBe CheckOutClosed
    resultReceivePayment.state shouldBe Closed
  }

  it should "not change state after cancel msg in completed state" in {
    val resultStartCheckout = eventSourcedTestKit.runCommand(StartCheckout)

    resultStartCheckout.event shouldBe CheckoutStarted
    resultStartCheckout.state.isInstanceOf[SelectingDelivery] shouldBe true

    val resultSelectDelivery = eventSourcedTestKit.runCommand(SelectDeliveryMethod(deliveryMethod))

    resultSelectDelivery.event.isInstanceOf[DeliveryMethodSelected] shouldBe true
    resultSelectDelivery.state.isInstanceOf[SelectingPaymentMethod] shouldBe true

    val resultSelectPayment = eventSourcedTestKit.runCommand(SelectPayment(paymentMethod, orderManagerProbe.ref))

    resultSelectPayment.event.isInstanceOf[PaymentStarted] shouldBe true
    resultSelectPayment.state.isInstanceOf[ProcessingPayment] shouldBe true

    val resultReceivePayment = eventSourcedTestKit.runCommand(ConfirmPaymentReceived)

    resultReceivePayment.event shouldBe CheckOutClosed
    resultReceivePayment.state shouldBe Closed

    val resultCancelCheckout = eventSourcedTestKit.runCommand(CancelCheckout)

    resultCancelCheckout.hasNoEvents shouldBe true
    resultCancelCheckout.state shouldBe Closed
  }
}
