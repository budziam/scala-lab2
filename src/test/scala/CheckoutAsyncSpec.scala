
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import reactive2.Checkout.{DeliveryMethodSelected, PaymentSelected}
import reactive2.PaymentService.PaymentReceived
import reactive2.{CartManager, Checkout, Customer}

class CheckoutAsyncSpec extends TestKit(ActorSystem("CheckoutAsyncSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  override def afterAll(): Unit = {
    system.terminate
  }

  "A checkout" must {
    "sends checkout closed to cart" in {
      import CartManager._
      val customer = system.actorOf(Props[Customer])
      val cart = TestProbe()
      val checkout = cart.childActorOf(Props(new Checkout(customer, cart.ref)))

      checkout ! DeliveryMethodSelected("testu")
      checkout ! PaymentSelected("yuhu")
      checkout ! PaymentReceived()
      cart.expectMsg(CheckoutClosed)
    }
  }
}

