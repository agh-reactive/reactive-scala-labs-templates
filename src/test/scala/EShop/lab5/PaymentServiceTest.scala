package EShop.lab5

import EShop.lab5.PaymentService.{PaymentClientError, PaymentServerError, PaymentSucceeded}
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._

class PaymentServiceTest
  extends TestKit(ActorSystem("PaymentServiceTest"))
  with AnyFlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)

  it should "response if external payment http server returned 200" in {
    val probe          = TestProbe()
    val paymentService = TestActorRef(PaymentService.props("payu", probe.ref))

    val msg = HttpResponse(StatusCodes.OK)
    paymentService.receive(msg, probe.ref)
    probe.expectMsg(PaymentSucceeded)
  }

  it should "fail if response from external payment http server returned 408" in {
    val probe   = TestProbe()
    val failure = TestProbe()

    val watcher = system.actorOf(Props(new Actor {

      val paymentService = context.actorOf(PaymentService.props("paypal", probe.ref))
      watch(paymentService)

      override def receive: Receive = {
        case _ => ()
      }

      override def supervisorStrategy: SupervisorStrategy =
        OneForOneStrategy(maxNrOfRetries = 1, withinTimeRange = 1.seconds) {
          case _: PaymentServerError =>
            failure.ref ! "failed"
            Stop
        }
    }))

    failure.expectMsg("failed")
  }

  it should "fail if response from external payment http server returned 404" in {
    val probe   = TestProbe()
    val failure = TestProbe()

    val watcher = system.actorOf(Props(new Actor {

      val paymentService = context.actorOf(PaymentService.props("someUnknownMethod", probe.ref))
      watch(paymentService)

      override def receive: Receive = {
        case _ => ()
      }

      override def supervisorStrategy: SupervisorStrategy =
        OneForOneStrategy(maxNrOfRetries = 1, withinTimeRange = 1.seconds) {
          case _: PaymentClientError =>
            failure.ref ! "failed"
            Stop
        }
    }))

    failure.expectMsg("failed")
  }

}
