package EShop.lab2

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {
  val system   = ActorSystem("system")
  val cart     = system.actorOf(Props[CartActor], "cart")
  val checkout = system.actorOf(Props[Checkout], "checkout")

  cart ! CartActor.AddItem("Mleko")
  cart ! CartActor.AddItem("Chrupki")
  cart ! CartActor.RemoveItem("Mleko")
  cart ! CartActor.StartCheckout

  checkout ! Checkout.StartCheckout
  checkout ! Checkout.SelectDeliveryMethod("shop")
  checkout ! Checkout.SelectPayment("cash")
  checkout ! Checkout.ConfirmPaymentReceived

  Await.result(system.whenTerminated, Duration.Inf)
}
