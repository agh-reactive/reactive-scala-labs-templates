package EShop.lab3

<<<<<<< HEAD
import EShop.lab2.{Cart, TypedCartActor}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
=======
<<<<<<<< HEAD:src/test/scala/EShop/lab3/CartTest.scala
import EShop.lab2.{Cart, CartActor}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
========
import EShop.lab2.{Cart, TypedCartActor}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
>>>>>>>> lab-3:src/test/scala/EShop/lab3/TypedCartTest.scala
>>>>>>> lab-3
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

  override def afterAll: Unit =
    testKit.shutdownTestKit()

  import TypedCartActor._

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
