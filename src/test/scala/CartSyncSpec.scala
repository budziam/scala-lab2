
import java.net.URI

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.WordSpecLike
import reactive2.{Cart, Item}

class CartSyncSpec extends TestKit(ActorSystem("CartSyncSpec"))
  with WordSpecLike {

  var cart: Cart = _
  var grass: Item = _
  var soap: Item = _
  var beer: Item = _

  def setUp(): Unit = {
    cart = Cart.empty
    grass = Item(URI.create("1"), "grass", 10, 1)
    soap = Item(URI.create("2"), "soap", 10, 1)
    beer = Item(URI.create("3"), "beer", 10, 1)
  }

  "A cart" must {

    "add one item" in {
      setUp()

      cart = cart.addItem(grass)

      assert(cart.items.size == 1)
    }

    "add and remove one item" in {
      setUp()

      cart = cart.addItem(grass)
      cart = cart.removeItem(grass, 1)

      assert(cart.items.isEmpty)
    }

    "add many items" in {
      setUp()

      cart = cart.addItem(grass)
      cart = cart.addItem(soap)
      cart = cart.addItem(beer)

      assert(cart.items.size == 3)
    }

    "add many items and remove some of them" in {
      setUp()

      cart = cart.addItem(grass)
      cart = cart.addItem(soap)
      cart = cart.addItem(beer)
      cart = cart.removeItem(soap, 1)

      assert(cart.items.size == 2)
    }

    "allow to remove only existing item" in {
      setUp()

      cart = cart.addItem(grass)
      cart = cart.removeItem(soap, 1)

      assert(cart.items.size == 1)
    }
  }
}

