package reactive2

import java.net.URI

import akka.actor._
import akka.event.LoggingReceive

object Customer {

  case class Init()

}

class Customer extends Actor {
  def receive = LoggingReceive {
    case Customer.Init() =>
      val cart = context.actorOf(Props(new CartManager(self)), "cart")

      val grass = Item(URI.create("1"), "grass", 10, 1)
      val soap = Item(URI.create("2"), "soap", 10, 1)
      val beer = Item(URI.create("3"), "beer", 10, 1)

      cart ! CartManager.AddItem(grass)
      cart ! CartManager.AddItem(soap)
      cart ! CartManager.AddItem(beer)
      cart ! CartManager.RemoveItem(grass, 1)
      cart ! CartManager.StartCheckOut

    case CartManager.CheckOutStarted(checkout) =>
      checkout ! Checkout.DeliveryMethodSelected()
      checkout ! Checkout.PaymentSelected()

    case Checkout.PaymentServiceStarted(paymentService) =>
      paymentService ! PaymentService.DoPayment()

    case CartManager.CartEmpty => println("[INFO] Oh no! Cart is empty")
    case CartManager.CheckoutClosed => println("[INFO] Oh no! Checkout is closed")
    case PaymentService.PaymentConfirmed() => println("[INFO] Oh yeah! Payment is confirmed")
  }
}
