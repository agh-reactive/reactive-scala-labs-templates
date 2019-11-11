package EShop.lab3

import EShop.lab2.CartActor._
import EShop.lab2.{Cart, CartActor, CartFSM}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CartTest
  extends TestKit(ActorSystem("CartTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  val item = "Test_Item"

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  //use GetItems command which was added to make test easier
  it should "add item properly" in {
    val actorRef = TestActorRef(CartFSM.props())
    actorRef ! AddItem(item)
    actorRef.receive(GetItems, self)
    expectMsg(Cart(Seq(item)))
  }

  it should "be empty after adding and removing the same item" in {
    val actorRef = TestActorRef(CartFSM.props())
    actorRef ! AddItem(item)
    actorRef ! RemoveItem(item)
    actorRef.receive(GetItems, self)
    expectMsg(Cart.empty)
  }

  it should "start checkout" in {
    val cartActor: TestActorRef[CartActor] = TestActorRef(new CartActor)
    cartActor ! AddItem(item)
    cartActor ! StartCheckout
    fishForMessage() {
      case _: CheckoutStarted => true
      case _                  => false
    }
  }
}
