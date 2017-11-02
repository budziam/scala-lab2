
import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import reactive2.{Cart, Customer, Item}

class CartSyncSpec extends TestKit(ActorSystem("CartSyncSpec"))
  with WordSpecLike with BeforeAndAfterAll {

  var customer: TestActorRef[Customer] = _
  var cart: TestActorRef[Cart] = _
  var grass: Item = _
  var soap: Item = _
  var beer: Item = _

  def setUp(): Unit = {
    customer = TestActorRef[Customer]
    cart = TestActorRef(new Cart(customer))
    grass = new Item("grass", 10)
    soap = new Item("soap", 10)
    beer = new Item("beer", 10)
  }

  "A cart" must {

    "add one item" in {
      import Cart._
      setUp()

      cart ! AddItem(grass)

      assert(cart.underlyingActor.items.size == 1)
    }

    "add and remove one item" in {
      import Cart._
      setUp()

      cart ! AddItem(grass)
      cart ! RemoveItem(grass)

      assert(cart.underlyingActor.items.isEmpty)
    }

    "add many items" in {
      import Cart._
      setUp()

      cart ! AddItem(grass)
      cart ! AddItem(soap)
      cart ! AddItem(beer)

      assert(cart.underlyingActor.items.size == 3)
    }

    "add many items and remove some of them" in {
      import Cart._
      setUp()

      cart ! AddItem(grass)
      cart ! AddItem(soap)
      cart ! AddItem(beer)
      cart ! RemoveItem(soap)

      assert(cart.underlyingActor.items.size == 2)
    }

    "allow to remove only existing item" in {
      import Cart._
      setUp()

      cart ! AddItem(grass)
      cart ! RemoveItem(soap)

      assert(cart.underlyingActor.items.size == 1)
    }
  }
}

