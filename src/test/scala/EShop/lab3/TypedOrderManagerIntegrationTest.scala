package EShop.lab3

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class TypedOrderManagerIntegrationTest
  extends ScalaTestWithActorTestKit
  with AnyFlatSpecLike
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  import TypedOrderManager._

  override implicit val timeout: Timeout = 1.second

  implicit val scheduler: Scheduler = testKit.scheduler

  def sendMessage(
    orderManager: ActorRef[TypedOrderManager.Command],
    message: ActorRef[Any] => TypedOrderManager.Command
  ): Unit = {
    import akka.actor.typed.scaladsl.AskPattern.Askable
    orderManager.ask[Any](message).mapTo[TypedOrderManager.Ack].futureValue shouldBe Done
  }

  it should "supervise whole order process" in {
    val orderManager = testKit.spawn(new TypedOrderManager().start).ref

    sendMessage(orderManager, AddItem("rollerblades", _))

    sendMessage(orderManager, Buy)

    sendMessage(orderManager, SelectDeliveryAndPaymentMethod("paypal", "inpost", _))

    sendMessage(orderManager, ref => Pay(ref))
  }

}
