package EShop.lab4

import EShop.lab2.Cart
import EShop.lab2.CartActor._
import akka.actor.{ActorRef, ActorSystem, Cancellable, PoisonPill, Props}
import akka.stream.Supervision.Stop
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}

import scala.concurrent.duration._
import scala.util.Random

/*
Please change tests below that persisting of state is tested. Remember that it is crucial to use the same persistenceId
for the actor to bring back his state. Use 'generatePersistenceId' to get Id, assign it to some val to use it afterwards
you terminate actor.
 */

class PersistentCartActorTest
  extends TestKit(ActorSystem("PersistentCartActorTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  import PersistentCartActorTest._

  it should "change state after adding first item to the cart" in {
    val nonEmptyTestMsg = "changedStateToNonEmpty"

    val cart = system.actorOf(
      Props(new PersistentCartActor(generatePersistenceId) {
        override def nonEmpty(cart: Cart, timer: Cancellable): Receive = {
          sender ! nonEmptyTestMsg
          super.nonEmpty(cart, timer)
        }
      }),
      "persistenceActor"
    )

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
    val id   = generatePersistenceId
    val cart = cartActorWithCartSizeResponseOnStateChange(system, id)

    cart ! AddItem("Storm")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    //restart actor
    cart ! PoisonPill
    val cartActorAfterRestart: ActorRef = cartActorWithCartSizeResponseOnStateChange(system, id)
    cartActorAfterRestart ! RemoveItem("Storm")
    expectMsg(emptyMsg)
    expectMsg(0)
  }

  it should "contain one item after adding new item and removing not existing one" in {
    val id: String = generatePersistenceId
    val cart       = cartActorWithCartSizeResponseOnStateChange(system, id)

    cart ! AddItem("Romeo & Juliet")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    //restart actor
    cart ! PoisonPill
    val cartActorAfterRestart: ActorRef = cartActorWithCartSizeResponseOnStateChange(system, id)
    cartActorAfterRestart ! RemoveItem("Makbet")
    expectNoMessage()
  }

  it should "change state to inCheckout from nonEmpty" in {
    val id: String = generatePersistenceId
    val cart       = cartActorWithCartSizeResponseOnStateChange(system, id)

    cart ! AddItem("Romeo & Juliet")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    //restart actor
    cart ! PoisonPill
    val cartActorAfterRestart: ActorRef = cartActorWithCartSizeResponseOnStateChange(system, id)
    cartActorAfterRestart ! StartCheckout
    fishForMessage() {
      case m: String if m == inCheckoutMsg => true
      case _: CheckoutStarted              => false
    }
    expectMsg(1)
  }

  it should "cancel checkout properly" in {
    val id: String = generatePersistenceId
    val cart       = cartActorWithCartSizeResponseOnStateChange(system, id)

    cart ! AddItem("Cymbelin")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    cart ! StartCheckout
    fishForMessage() {
      case m: String if m == inCheckoutMsg => true
      case _: CheckoutStarted              => false
    }
    expectMsg(1)
    //restart actor
    cart ! PoisonPill
    val cartActorAfterRestart: ActorRef = cartActorWithCartSizeResponseOnStateChange(system, id)
    cartActorAfterRestart ! CancelCheckout
    expectMsg(nonEmptyMsg)
    expectMsg(1)
  }

  it should "close checkout properly" in {
    val id: String = generatePersistenceId
    val cart       = cartActorWithCartSizeResponseOnStateChange(system, id)

    cart ! AddItem("Cymbelin")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    cart ! StartCheckout
    fishForMessage() {
      case m: String if m == inCheckoutMsg => true
      case _: CheckoutStarted              => false
    }
    expectMsg(1)
    //restart actor
    cart ! PoisonPill
    val cartActorAfterRestart: ActorRef = cartActorWithCartSizeResponseOnStateChange(system, id)
    cartActorAfterRestart ! CloseCheckout
    expectMsg(emptyMsg)
    expectMsg(0)
  }

  it should "not add items when in checkout" in {
    val id: String = generatePersistenceId
    val cart       = cartActorWithCartSizeResponseOnStateChange(system, id)

    cart ! AddItem("Cymbelin")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    cart ! StartCheckout
    fishForMessage() {
      case m: String if m == inCheckoutMsg => true
      case _: CheckoutStarted              => false
    }
    expectMsg(1)
    //restart actor
    cart ! PoisonPill
    val cartActorAfterRestart: ActorRef = cartActorWithCartSizeResponseOnStateChange(system, id)
    cartActorAfterRestart ! AddItem("Henryk V")
    expectNoMessage
  }

  it should "not change state to inCheckout from empty" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! StartCheckout
    expectNoMessage()
  }

  it should "expire and back to empty state after given time" in {
    val id: String = generatePersistenceId
    val cart       = cartActorWithCartSizeResponseOnStateChange(system, id)

    cart ! AddItem("King Lear")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
    //restart actor
    cart ! PoisonPill
    val cartActorAfterRestart: ActorRef = cartActorWithCartSizeResponseOnStateChange(system, id)
    Thread.sleep(1500)
    cartActorAfterRestart ! AddItem("King Lear")
    expectMsg(nonEmptyMsg)
    expectMsg(1)
  }
}

object PersistentCartActorTest {
  val emptyMsg      = "empty"
  val nonEmptyMsg   = "nonEmpty"
  val inCheckoutMsg = "inCheckout"

  def generatePersistenceId = Random.alphanumeric.take(256).mkString

  def cartActorWithCartSizeResponseOnStateChange(
    system: ActorSystem,
    persistenceId: String = generatePersistenceId
  ): ActorRef =
    system.actorOf(Props(new PersistentCartActor(persistenceId) {
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
