package EShop.lab2

import EShop.lab2.CartActor.{AddItem, CancelCheckout, CloseCheckout, RemoveItem, StartCheckout}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}
import EShop.lab2.CartFSM.Status._

class CartFSMTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  import CartFSMTest._

  it should "contain one item after adding new item" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Otello")
    expectMsg(nonEmptyMsg)
    expectMsg(0)
  }

  it should "be empty after adding new item and removing it after that" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Storm")
    expectMsg(nonEmptyMsg)
    expectMsg(0)
    cart ! RemoveItem("Storm")
    expectMsg(emptyMsg)
    expectMsg(1)
  }

  it should "contain one item after adding new item and removing not existing one" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Romeo & Juliet")
    expectMsg(nonEmptyMsg)
    expectMsg(0)
    cart ! RemoveItem("Makbet")
    expectNoMessage()
  }

  it should "change state to inCheckout from nonEmpty" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Romeo & Juliet")
    expectMsg(nonEmptyMsg)
    expectMsg(0)
    cart ! StartCheckout
    expectMsg(inCheckoutMsg)
    expectMsg(1)
  }

  it should "cancel checkout properly" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Cymbelin")
    expectMsg(nonEmptyMsg)
    expectMsg(0)
    cart ! StartCheckout
    expectMsg(inCheckoutMsg)
    expectMsg(1)
    cart ! CancelCheckout
    expectMsg(nonEmptyMsg)
    expectMsg(1)
  }

  it should "close checkout properly" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Cymbelin")
    expectMsg(nonEmptyMsg)
    expectMsg(0)
    cart ! StartCheckout
    expectMsg(inCheckoutMsg)
    expectMsg(1)
    cart ! CloseCheckout
    expectMsg(emptyMsg)
    expectMsg(1)
  }

  it should "not add items when in checkout" in {
    val cart = cartActorWithCartSizeResponseOnStateChange(system)

    cart ! AddItem("Cymbelin")
    expectMsg(nonEmptyMsg)
    expectMsg(0)
    cart ! StartCheckout
    expectMsg(inCheckoutMsg)
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
    expectMsg(0)
    Thread.sleep(2000)
    cart ! AddItem("King Lear")
    expectMsg(nonEmptyMsg)
    expectMsg(0)
  }

}

object CartFSMTest {

  val emptyMsg      = "empty"
  val nonEmptyMsg   = "nonEmpty"
  val inCheckoutMsg = "inCheckout"

  def cartActorWithCartSizeResponseOnStateChange(system: ActorSystem) =
    system.actorOf(Props(new CartFSM {

      onTransition {
        case Empty -> NonEmpty => {
          sender ! nonEmptyMsg
          sender ! stateData.size
        }
        case NonEmpty -> InCheckout => {
          sender ! inCheckoutMsg
          sender ! stateData.size
        }
        case NonEmpty -> Empty => {
          sender ! emptyMsg
          sender ! stateData.size
        }
        case InCheckout -> NonEmpty => {
          sender ! nonEmptyMsg
          sender ! stateData.size
        }
        case InCheckout -> Empty =>
          sender ! emptyMsg
          sender ! stateData.size
      }
    }))
}
