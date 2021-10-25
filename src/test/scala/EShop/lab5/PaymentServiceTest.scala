package EShop.lab5

import EShop.lab5.PaymentService.{PaymentClientError, PaymentServerError, PaymentSucceeded}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ChildFailed
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.concurrent.Future

class PaymentServiceTest extends ScalaTestWithActorTestKit with AnyFlatSpecLike {

  it should "response if external payment http server returned 200" in {
    val probe = testKit.createTestProbe[PaymentService.Response]()
    val paymentService =
      testKit.spawn(PaymentService("visa", probe.ref))

    probe.expectMessage(PaymentSucceeded)
  }

  it should "fail if response from external payment http server returned 408 (Request Timeout)" in {
    val probe   = testKit.createTestProbe[PaymentService.Response]()
    val failure = testKit.createTestProbe[String]()

    testKit.spawn(Behaviors.setup[Any] { context =>
      val paymentService = context.spawn(PaymentService("paypal", probe.ref), "PaymentService")
      context.watch(paymentService)

      Behaviors.receiveSignal[Any] {
        case (context, cf: ChildFailed) if cf.cause == PaymentServerError() =>
          failure.ref ! "failed"
          Behaviors.same
      }
    })

    failure.expectMessage("failed")
  }

  it should "fail if response from external payment http server returned 404" in {
    val probe   = testKit.createTestProbe[PaymentService.Response]()
    val failure = testKit.createTestProbe[String]()

    testKit.spawn(Behaviors.setup[Any] { context =>
      val paymentService = context.spawn(PaymentService("someUnknownMethod", probe.ref), "PaymentService")
      context.watch(paymentService)

      Behaviors.receiveSignal[Any] {
        case (context, cf: ChildFailed) if cf.cause == PaymentClientError() =>
          failure.ref ! "failed"
          Behaviors.same
      }
    })

    failure.expectMessage("failed")
  }
}
