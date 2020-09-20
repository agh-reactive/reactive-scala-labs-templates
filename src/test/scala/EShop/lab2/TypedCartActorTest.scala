package EShop.lab2

import EShop.lab3.TypedOrderManager
import akka.actor.Cancellable
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._

class TypedCartActorTest extends ScalaTestWithActorTestKit with AnyFlatSpecLike with BeforeAndAfterAll {

  override def afterAll: Unit = testKit.shutdownTestKit()

  import TypedCartActorTest._
  import TypedCartActor._

  it should "change state after adding first item to the cart" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("Hamlet")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)
  }

  it should "be empty after adding new item and removing it after that" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("Storm")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)

    cart ! RemoveItem("Storm")

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)
  }

  it should "contain one item after adding new item and removing not existing one" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("Romeo & Juliet")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)

    cart ! RemoveItem("Makbet")

    probe.expectNoMessage()
  }

  it should "change state to inCheckout from nonEmpty" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("Romeo & Juliet")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)

    cart ! StartCheckout(testKit.createTestProbe[TypedOrderManager.Command]().ref)

    probe.expectMessage(inCheckoutMsg)
    probe.expectMessage(1)
  }

  it should "cancel checkout properly" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("Cymbelin")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)

    cart ! StartCheckout(testKit.createTestProbe[TypedOrderManager.Command]().ref)

    probe.expectMessage(inCheckoutMsg)
    probe.expectMessage(1)

    cart ! ConfirmCheckoutCancelled

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)
  }

  it should "close checkout properly" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("Cymbelin")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)

    cart ! StartCheckout(testKit.createTestProbe[TypedOrderManager.Command]().ref)

    probe.expectMessage(inCheckoutMsg)
    probe.expectMessage(1)

    cart ! ConfirmCheckoutClosed

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)
  }

  it should "not add items when in checkout" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("Cymbelin")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)

    cart ! StartCheckout(testKit.createTestProbe[TypedOrderManager.Command]().ref)

    probe.expectMessage(inCheckoutMsg)
    probe.expectMessage(1)

    cart ! AddItem("Henryk V")

    probe.expectNoMessage()
  }

  it should "not change state to inCheckout from empty" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! StartCheckout(testKit.createTestProbe[TypedOrderManager.Command]().ref)

    probe.expectNoMessage()
  }

  it should "expire and back to empty state after given time" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("King Lear")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)

    Thread.sleep(1500)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("King Lear")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)
  }
}

object TypedCartActorTest {
  val emptyMsg      = "empty"
  val nonEmptyMsg   = "nonEmpty"
  val inCheckoutMsg = "inCheckout"

  def cartActorWithCartSizeResponseOnStateChange(
    testKit: ActorTestKit,
    probe: ActorRef[Any]
  ): ActorRef[TypedCartActor.Command] =
    testKit.spawn {
      val cartActor = new TypedCartActor {
        override val cartTimerDuration: FiniteDuration = 1.seconds

        override def empty: Behavior[TypedCartActor.Command] =
          Behaviors.setup(_ => {
            probe ! emptyMsg
            probe ! 0
            super.empty
          })

        override def nonEmpty(cart: Cart, timer: Cancellable): Behavior[TypedCartActor.Command] =
          Behaviors.setup(_ => {
            probe ! nonEmptyMsg
            probe ! cart.size
            super.nonEmpty(cart, timer)
          })

        override def inCheckout(cart: Cart): Behavior[TypedCartActor.Command] =
          Behaviors.setup(_ => {
            probe ! inCheckoutMsg
            probe ! cart.size
            super.inCheckout(cart)
          })

      }
      cartActor.start
    }

}
