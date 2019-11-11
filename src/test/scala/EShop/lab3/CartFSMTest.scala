package EShop.lab3

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CartFSMTest
  extends TestKit(ActorSystem("CartTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  //use GetItems command which was added to make test easier
  it should "add item properly" in {
    ???
  }

  it should "be empty after adding and removing the same item" in {
    ???
  }

  it should "start checkout" in {
    ???
  }
}
