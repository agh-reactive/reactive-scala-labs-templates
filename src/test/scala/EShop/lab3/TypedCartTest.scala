package EShop.lab3

import EShop.lab2.{Cart, TypedCartActor}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class TypedCartTest
  extends ScalaTestWithActorTestKit
  with AnyFlatSpecLike
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  override def afterAll(): Unit = testKit.shutdownTestKit()

  import TypedCartActor._

  it should "add item properly" in {
    val cartActor = testKit.spawn(TypedCartActor())
    val probe = testKit.createTestProbe[Cart]()
    val item = "item"

    cartActor ! AddItem(item)
    cartActor ! GetItems(probe.ref)
    probe.expectMessage(Cart(Seq(item)))
  }

  it should "be empty after adding and removing the same item" in {
    val cartActor = testKit.spawn(TypedCartActor())
    val probe = testKit.createTestProbe[Cart]()
    val item = "item"

    cartActor ! AddItem(item)
    cartActor ! RemoveItem(item)
    cartActor ! GetItems(probe.ref)
    probe.expectMessage(Cart(Seq.empty))
  }

  it should "start checkout" in {
    val cartActor = testKit.spawn(TypedCartActor())
    val probe = testKit.createTestProbe[OrderManager.Command]()

    cartActor ! AddItem("item")
    cartActor ! StartCheckout(probe.ref)
    probe.receiveMessage() shouldBe a[OrderManager.ConfirmCheckoutStarted]
  }
}
