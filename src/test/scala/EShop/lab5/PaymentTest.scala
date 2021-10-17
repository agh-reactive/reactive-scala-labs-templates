package EShop.lab5

import EShop.lab2.TypedCheckout
import EShop.lab3.OrderManager
import EShop.lab3.Payment.DoPayment
import PaymentServiceServer.PaymentServiceServer
import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentTest extends ScalaTestWithActorTestKit with AnyFlatSpecLike {

  it should "properly confirm payment after 2 retries using payu payment method" in {
    val manager  = testKit.createTestProbe[OrderManager.Command]()
    val checkout = testKit.createTestProbe[TypedCheckout.Command]()
    val payment  = testKit.spawn(Payment("payu", manager.ref, checkout.ref))

    val server = new PaymentServiceServer()
    Future {
      server.run()
    }

    payment ! Payment.DoPayment

    manager.expectMessage(OrderManager.ConfirmPaymentReceived)

    server.system.terminate()
  }

  it should "stop the payment process if the client request results in NotFound" in {
    val manager  = testKit.createTestProbe[OrderManager.Command]()
    val checkout = testKit.createTestProbe[TypedCheckout.Command]()
    val payment  = testKit.spawn(Payment("notfound", manager.ref, checkout.ref))

    val server = new PaymentServiceServer()
    Future {
      server.run()
    }

    payment ! Payment.DoPayment

    manager.expectMessage(OrderManager.PaymentRejected)

    server.system.terminate()
  }

}
