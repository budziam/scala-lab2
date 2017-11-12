
import java.net.URI

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import reactive2.{CartManager, Item}

class CartAsyncSpec extends TestKit(ActorSystem("CartAsyncSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  var customer: TestProbe = _
  var cart: ActorRef = _
  var grass: Item = _

  override def afterAll(): Unit = {
    system.terminate
  }

  def setUp(): Unit = {
    customer = TestProbe()
    cart = system.actorOf(Props(new CartManager(customer.ref) {
      override def createCheckoutActor(): ActorRef = super.createCheckoutActor()
    }))
    grass = Item(URI.create("1"), "grass", 10, 1)
  }

  "A cart" must {
    "starts checkout when is not empty and someone wants to start checkout" in {
      import CartManager._
      setUp()

      val checkout = TestProbe().ref
      cart = system.actorOf(Props(new CartManager(customer.ref) {
        override def createCheckoutActor(): ActorRef = checkout
      }))

      cart ! AddItem(grass)
      cart ! StartCheckOut()
      expectMsg(CheckOutStarted(checkout))
    }

    "inform about empty items when last item is removed" in {
      import CartManager._
      setUp()

      cart ! AddItem(grass)
      cart ! RemoveItem(grass, 1)
      customer.expectMsg(CartEmpty())
    }

    "inform about empty items when cart timer expired" in {
      import CartManager._
      setUp()

      cart ! AddItem(grass)
      cart ! CartTimerExpired()
      customer.expectMsg(CartEmpty())
    }

    "inform about empty items when checkout has closed" in {
      import CartManager._
      setUp()

      cart ! AddItem(grass)
      cart ! StartCheckOut()
      cart ! CheckoutClosed()
      customer.expectMsg(CartEmpty())
    }
  }
}

