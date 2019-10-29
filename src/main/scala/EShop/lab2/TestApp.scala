package EShop.lab2
import EShop.lab2.CartActor.{AddItem, CloseCheckout, RemoveItem}
import EShop.lab2.Checkout._
import akka.actor.{ActorSystem, Props}

object TestApp extends App {

  val system = ActorSystem("Reactive")

  val cartActor = system.actorOf(Props[CartActor], "cartActor")

  cartActor ! AddItem("X")
  cartActor ! AddItem("Y")
  cartActor ! RemoveItem("X")
  cartActor ! RemoveItem("Z")
  cartActor ! AddItem("X")
  cartActor ! StartCheckout
  cartActor ! CancelCheckout
  cartActor ! StartCheckout
  cartActor ! CloseCheckout

  val cartFSMActor = system.actorOf(Props[CartFSM], "cartFSMActor")

  cartFSMActor ! AddItem("X")
  cartFSMActor ! AddItem("Y")
  cartFSMActor ! RemoveItem("X")
  cartFSMActor ! RemoveItem("Z")
  cartFSMActor ! AddItem("X")
  cartFSMActor ! StartCheckout
  cartFSMActor ! CancelCheckout
  cartFSMActor ! StartCheckout
  cartFSMActor ! CloseCheckout

  val checkoutActor = system.actorOf(Props[Checkout], "checkoutActor")

  checkoutActor ! StartCheckout
  checkoutActor ! SelectDeliveryMethod("X")
  checkoutActor ! CancelCheckout
  checkoutActor ! StartCheckout
  checkoutActor ! SelectDeliveryMethod("Y")
  checkoutActor ! SelectPayment("Z")
  checkoutActor ! ReceivePayment
  checkoutActor ! CancelCheckout
  checkoutActor ! CancelCheckout

  val checkoutFSMActor = system.actorOf(Props[CheckoutFSM], "checkoutFSMActor")

  checkoutFSMActor ! StartCheckout
  checkoutFSMActor ! SelectDeliveryMethod("X")
  checkoutFSMActor ! CancelCheckout
  checkoutFSMActor ! StartCheckout
  checkoutFSMActor ! SelectDeliveryMethod("Y")
  checkoutFSMActor ! SelectPayment("Z")
  checkoutFSMActor ! ReceivePayment
  checkoutFSMActor ! CancelCheckout
  checkoutFSMActor ! CancelCheckout

}
