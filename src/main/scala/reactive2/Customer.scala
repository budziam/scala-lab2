package reactive2

import akka.actor._
import akka.event.LoggingReceive

object Customer {

  case class Init()

}

class Customer extends Actor {
  def receive = LoggingReceive {
    case Customer.Init() =>
      val cart = context.actorOf(Props(new Cart(self)), "cart")

      val grass = new Item("grass", 10)
      val soap = new Item("soap", 10)
      val beer = new Item("beer", 10)

      cart ! Cart.AddItem(grass)
      cart ! Cart.AddItem(soap)
      cart ! Cart.AddItem(beer)
      cart ! Cart.RemoveItem(grass)
      cart ! Cart.StartCheckOut()

    case Cart.CheckOutStarted(checkout) =>
      checkout ! Checkout.DeliveryMethodSelected()
      checkout ! Checkout.PaymentSelected()

    case Checkout.PaymentServiceStarted(paymentService) =>
      paymentService ! PaymentService.DoPayment()

    case Cart.CartEmpty() => println("[INFO] Oh no! Cart is empty")
    case Cart.CheckoutClosed() => println("[INFO] Oh no! Checkout is closed")
    case PaymentService.PaymentConfirmed() => println("[INFO] Oh yeah! Payment is confirmed")
  }
}
