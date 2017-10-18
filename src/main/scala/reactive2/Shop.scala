package reactive2

import akka.actor._
import akka.event.LoggingReceive

import scala.concurrent.Await
import scala.concurrent.duration._

object Shop {

  case object Init

}

class Shop extends Actor {
  def receive = LoggingReceive {
    case Shop.Init =>
      val cart = context.actorOf(Props[Cart], "cart")
      val grass1 = new Item("grass", 10)
      val grass2 = new Item("grass", 10)
      val grass3 = new Item("grass", 10)

      cart ! Cart.ItemAdded(grass1)
      cart ! Cart.ItemAdded(grass2)
      cart ! Cart.ItemAdded(grass3)
      cart ! Cart.ItemRemoved(grass1)
      cart ! Cart.CheckoutStarted

    case Cart.CheckoutCreated(checkout) =>
      checkout ! Checkout.DeliveryMethodSelected
      checkout ! Checkout.PaymentSelected
      checkout ! Checkout.PaymentReceived
  }
}

object ShopApp extends App {
  val system = ActorSystem("shopApp")
  val shopActor = system.actorOf(Props[Shop], "shop")

  shopActor ! Shop.Init

  Await.result(system.whenTerminated, Duration.Inf)
}
