package EShop.lab3

import EShop.lab2.{CartActor, Checkout}
import EShop.lab3.OrderManager._
import EShop.lab3.Payment.DoPayment
import akka.actor.FSM

class OrderManagerFSM extends FSM[State, Data] {

  startWith(Uninitialized, Empty)

  when(Uninitialized) {
    case Event(AddItem(item), _) => {
      val cartActor = context.system.actorOf(CartActor.props(), "CartActor")
      cartActor ! CartActor.AddItem(item)
      sender ! Done
      goto(Open) using CartData(cartActor)
    }
  }

  when(Open) {
    case Event(AddItem(item), CartData(cartActor: CartActor)) => {
      cartActor ! CartActor.AddItem(item)
      sender ! Done
      stay
    }
    case Event(RemoveItem(item), CartData(cartActor: CartActor)) => {
      cartActor ! CartActor.RemoveItem(item)
      sender ! Done
      stay
    }
    case Event(Buy, CartData(cartActor: CartActor)) => {
      cartActor ! CartActor.StartCheckout
      goto(InCheckout) using CartDataWithSender(cartActor, sender)
    }
  }

  when(InCheckout) {
    case Event(CartActor.CheckoutStarted(checkoutRef), CartDataWithSender(_, senderRef)) => {
      senderRef ! Done
      stay using InCheckoutData(checkoutRef)
    }
    case Event(SelectDeliveryAndPaymentMethod(delivery, payment), InCheckoutData(checkoutRef)) => {
      checkoutRef ! Checkout.SelectDeliveryMethod(delivery)
      checkoutRef ! Checkout.SelectPayment(payment)
      goto(InPayment) using (InCheckoutDataWithSender(checkoutRef, sender))
    }
  }

  when(InPayment) {
    case Event(Checkout.PaymentStarted(paymentRef), InCheckoutDataWithSender(_, senderRef)) => {
      senderRef ! Done
      stay using InPaymentData(paymentRef)
    }
    case Event(Pay, InPaymentData(paymentRef)) => {
      paymentRef ! DoPayment
      stay using InPaymentDataWithSender(paymentRef, sender)
    }
    case Event(Payment.PaymentConfirmed, InPaymentDataWithSender(_, senderRef)) => {
      senderRef ! Done
      goto(Finished)
    }
  }

  when(Finished) {
    case _ =>
      sender ! "order manager finished job"
      stay
  }

}
