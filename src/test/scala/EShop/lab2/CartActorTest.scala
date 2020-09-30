package EShop.lab2

import EShop.lab2.CartActor.{
  AddItem,
  CheckoutStarted,
  ConfirmCheckoutCancelled,
  ConfirmCheckoutClosed,
  RemoveItem,
  StartCheckout
}
import EShop.lab3.OrderManager
import akka.actor.{ActorRef, ActorSystem, Cancellable, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike

import scala.concurrent.duration._

class CartActorTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  import CartActorTest._

  it should "change state after adding first item to the cart" in {
    val nonEmptyTestMsg = "changedStateToNonEmpty"

    val cart = system.actorOf(Props(new CartActor {
      override def nonEmpty(cart: Cart, timer: Cancellable): Receive = {
        sender ! nonEmptyTestMsg
        super.nonEmpty(cart, timer)
      }
    }))

    cart ! AddItem("Hamlet")
    expectMsg(nonEmptyTestMsg)
  }

  it should "contain one item after adding new item" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Otello")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
  }

  it should "be empty after adding new item and removing it after that" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Storm")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    cart ! RemoveItem("Storm")
    expectMsg(emptyMsg)
    expectMsg(0)
  }

  it should "contain one item after adding new item and removing not existing one" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Romeo & Juliet")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    cart ! RemoveItem("Makbet")
    expectNoMessage()
  }

  it should "change state to inCheckout from nonEmpty" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Romeo & Juliet")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    cart ! StartCheckout
    fishForMessage() {
      case m: String if m == inCheckoutMsg        => true
      case _: OrderManager.ConfirmCheckoutStarted => false
    }
    expectMsg(1)
  }

  it should "cancel checkout properly" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Cymbelin")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    cart ! StartCheckout
    fishForMessage() {
      case m: String if m == inCheckoutMsg        => true
      case _: OrderManager.ConfirmCheckoutStarted => false
    }
    expectMsg(1)
    cart ! ConfirmCheckoutCancelled
    expectMsg(nonEmptyMsg)
    expectMsg(1)
  }

  it should "close checkout properly" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Cymbelin")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    cart ! StartCheckout
    fishForMessage() {
      case m: String if m == inCheckoutMsg        => true
      case _: OrderManager.ConfirmCheckoutStarted => false
    }
    expectMsg(1)
    cart ! ConfirmCheckoutClosed
    expectMsg(emptyMsg)
    expectMsg(0)
  }

  it should "not add items when in checkout" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Cymbelin")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    cart ! StartCheckout
    fishForMessage() {
      case m: String if m == inCheckoutMsg        => true
      case _: OrderManager.ConfirmCheckoutStarted => false
    }
    expectMsg(1)
    cart ! AddItem("Henryk V")
    expectNoMessage
  }

  it should "not change state to inCheckout from empty" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! StartCheckout
    expectNoMessage()
  }

  it should "expire and back to empty state after given time" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("King Lear")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    Thread.sleep(1500)
    cart ! AddItem("King Lear")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
  }
}

object CartActorTest {
  val emptyMsg      = "empty"
  val nonEmptyMsg   = "nonEmpty"
  val inCheckoutMsg = "inCheckout"

  def cartActorWithCartSizeResponseOnStateChange(system: ActorSystem): ActorRef =
    system.actorOf(Props(new CartActor {
      override val cartTimerDuration: FiniteDuration = 1.seconds

      override def empty() = {
        val result = super.empty
        sender ! emptyMsg
        sender ! 0
        result
      }

      override def nonEmpty(cart: Cart, timer: Cancellable): Receive = {
        val result = super.nonEmpty(cart, timer)
        sender ! nonEmptyMsg
        sender ! cart.size
        result
      }

      override def inCheckout(cart: Cart): Receive = {
        val result = super.inCheckout(cart)
        sender ! inCheckoutMsg
        sender ! cart.size
        result
      }

    }))

}
