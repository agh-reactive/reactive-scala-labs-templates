package EShop.lab3

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

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)
    val item = "Item"
  //use GetItems command which was added to make test easier
  it should "add item properly" in {
    val cartRef = TestActorRef(CartActor.props)
    cartRef ! CartActor.AddItem(item)
    cartRef ! CartActor.GetCart
    expectMsg(Cart(Seq(item)))
  }

  it should "be empty after adding and removing the same item" in {
    val cartRef = TestActorRef(CartActor.props)
    cartRef ! CartActor.AddItem(item)
    cartRef ! CartActor.RemoveItem(item)
    cartRef ! CartActor.GetCart
    expectMsg(Cart.empty)
  }

  it should "start checkout" in {
    val cartRef = TestActorRef(CartFSM.props())
    cartRef ! CartActor.AddItem(item)
    cartRef ! CartActor.StartCheckout
    expectMsgPF(){
      case CartActor.CheckoutStarted(_) => ()
      case _ => fail()
    }
  }
}
