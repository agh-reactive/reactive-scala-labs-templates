package EShop.lab3

import EShop.lab2.CartActor.CloseCheckout
import EShop.lab2.Checkout.PaymentStarted
import EShop.lab2.{Checkout, CheckoutFSM}
import EShop.lab3.Payment.DoPayment
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CheckoutFSMTest
  extends TestKit(ActorSystem("CheckoutTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  it should "Send close confirmation to cart" in {
    val checkoutActor = TestActorRef(new CheckoutFSM(self))
    checkoutActor ! Checkout.StartCheckout
    checkoutActor ! Checkout.SelectDeliveryMethod("test delivery")
    checkoutActor ! Checkout.SelectPayment("test payment")
    val payment = expectMsgPF() {
      case PaymentStarted(paymentService) => paymentService
    }
    payment ! DoPayment
    expectMsg(CloseCheckout)
  }

}
