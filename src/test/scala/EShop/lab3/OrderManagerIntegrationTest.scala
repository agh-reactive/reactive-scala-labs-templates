package EShop.lab3

import EShop.lab3.OrderManager._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class OrderManagerIntegrationTest
  extends TestKit(ActorSystem("OrderManagerIntegrationTest"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  implicit val timeout: Timeout = 1.second

  it should "supervise whole order process" in {

    def sendMessage(
      orderManager: TestActorRef[OrderManager],
      message: OrderManager.Command
    ): Unit =
      (orderManager ? message).mapTo[OrderManager.Ack].futureValue shouldBe Done

    val orderManager = TestActorRef(new OrderManager())

    sendMessage(orderManager, AddItem("rollerblades"))

    sendMessage(orderManager, Buy)

    sendMessage(orderManager, SelectDeliveryAndPaymentMethod("paypal", "inpost"))

    sendMessage(orderManager, Pay)

    (orderManager ? Pay).mapTo[String].futureValue shouldBe "order manager finished job"
  }

}
