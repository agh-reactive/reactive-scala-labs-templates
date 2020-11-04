package EShop.lab3

import EShop.lab2.CartActor.ConfirmCheckoutClosed
import EShop.lab2.{CartActor, Checkout}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class CheckoutTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  val cartActor: TestProbe = TestProbe()

  import Checkout._

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  it should "Send close confirmation to cart" in {
    val checkoutActor = system.actorOf(Props(new Checkout(cartActor.ref)))

    checkoutActor ! StartCheckout
    checkoutActor ! SelectDeliveryMethod("")
    checkoutActor ! SelectPayment("")
    checkoutActor ! ConfirmPaymentReceived

    cartActor.expectMsg(ConfirmCheckoutClosed)
  }

}
