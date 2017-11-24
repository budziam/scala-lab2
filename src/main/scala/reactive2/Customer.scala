package reactive2

import java.net.URI

import akka.actor._
import akka.event.LoggingReceive
import reactive2.ProductCatalogMessages.BestMatches

object Customer {

  case class Purchase()

}

class Customer extends Actor {
  private val catalog = context.system.actorSelection("akka.tcp://productCatalog@127.0.0.1:2553/user/productCatalog")

  def receive = LoggingReceive {
    case Customer.Purchase() =>
      val cartManager = context.actorOf(Props(new CartManager(self)), "cartManager")

      val grass = Item(URI.create("1"), "grass", 10, 1)
      val soap = Item(URI.create("2"), "soap", 10, 1)
      val beer = Item(URI.create("3"), "beer", 10, 1)

      cartManager ! CartManager.AddItem(grass)
      cartManager ! CartManager.AddItem(soap)
      cartManager ! CartManager.AddItem(beer)
      cartManager ! CartManager.RemoveItem(grass, 1)
      cartManager ! CartManager.StartCheckOut

    case CartManager.CheckOutStarted(checkout) =>
      checkout ! Checkout.DeliveryMethodSelected("abcd")
      checkout ! Checkout.PaymentSelected("poiu")

    case Checkout.PaymentServiceStarted(paymentService) =>
      paymentService ! PaymentService.DoPayment("stripe")

    case CartManager.CartEmpty => println("[INFO] Oh no! Cart is empty")
    case CartManager.CheckoutClosed => println("[INFO] Oh no! Checkout is closed")
    case PaymentService.PaymentConfirmed() => println("[INFO] Oh yeah! Payment is confirmed")

    case msg: BestMatches => catalog ! msg
    case result: List[Item] => result.foreach(println)
  }
}
