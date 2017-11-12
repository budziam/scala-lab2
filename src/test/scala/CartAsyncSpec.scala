
import java.net.URI

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import reactive2.{CartManager, Item}

class CartAsyncSpec extends TestKit(ActorSystem("CartAsyncSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  var customer: TestProbe = _
  var cartManager: ActorRef = _
  var grass: Item = _

  override def afterAll(): Unit = {
    system.terminate
  }

  def setUp(): Unit = {
    customer = TestProbe()
    cartManager = system.actorOf(Props(new CartManager(customer.ref, String.valueOf(System.currentTimeMillis()))))
    grass = Item(URI.create("1"), "grass", 10, 1)
  }

  "A cart" must {
    "starts checkout when is not empty and someone wants to start checkout" in {
      import CartManager._
      setUp()

      val checkout = TestProbe().ref
      cartManager = system.actorOf(Props(new CartManager(customer.ref) {
        override def createCheckoutActor(): ActorRef = checkout
      }))

      cartManager ! AddItem(grass)
      cartManager ! StartCheckOut
      expectMsg(CheckOutStarted(checkout))
    }

    "inform about empty items when last item is removed" in {
      import CartManager._
      setUp()

      cartManager ! AddItem(grass)
      cartManager ! RemoveItem(grass, 1)
      customer.expectMsg(CartEmpty)
    }

    "inform about empty items when cart timer expired" in {
      import CartManager._
      setUp()

      cartManager ! AddItem(grass)
      cartManager ! CartTimerExpired
      customer.expectMsg(CartEmpty)
    }

    "inform about empty items when checkout has closed" in {
      import CartManager._
      setUp()

      cartManager ! AddItem(grass)
      cartManager ! StartCheckOut
      cartManager ! CheckoutClosed
      customer.expectMsg(CartEmpty)
    }
  }
}

