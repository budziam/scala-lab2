package reactive2

import java.net.URI

import akka.actor.{ActorRef, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence._

import scala.concurrent.duration._

case class Item(id: URI, name: String, price: BigDecimal, count: Int)

object Cart {
  val empty = Cart(Map.empty)
}

case class Cart(items: Map[URI, Item]) {
  def addItem(item: Item): Cart = {
    copy(items = items.updated(item.id, item.copy(count = getItemCount(item) + item.count)))
  }

  def removeItem(item: Item, count: Int): Cart = {
    val itemCount = getItemCount(item)
    if (itemCount <= count) {
      return copy(items = items - item.id)
    }

    copy(items = items.updated(item.id, item.copy(count = itemCount - count)))
  }

  private def getItemCount(it: Item): Int = {
    if (items contains it.id) items(it.id).count else 0
  }
}

object CartManager {

  case class AddItem(item: Item)

  case class RemoveItem(item: Item, count: Int)

  case class StartCheckOut()

  case class CheckoutCancelled()

  case class CheckoutClosed()

  case class CartTimerExpired()

  case class CheckOutStarted(checkout: ActorRef)

  case class CartEmpty()

  case object CartTimerKey

  case object Snap

}

class CartManager(customer: ActorRef) extends PersistentActor with Timers {

  import CartManager._

  override def persistenceId = "cart-1"

  var cart: Cart = Cart.empty

  def receiveCommand: Receive = empty

  val receiveRecover: Receive = {
    case AddItem(item: Item) =>
      cart = cart.addItem(item)
      changeContextToNonEmpty()

    case SnapshotOffer(_, snapshot: Cart) => cart = snapshot
  }

  def empty: Receive = LoggingReceive {
    case AddItem(item: Item) =>
      cart = cart.addItem(item)
      changeContextToNonEmpty()

    case Snap => saveSnapshot(cart)
  }

  def nonEmpty: Receive = LoggingReceive {
    case AddItem(item: Item) =>
      cart = cart.addItem(item)

    case RemoveItem(item: Item, count: Int) if cart.items.size > 1 =>
      cart = cart.removeItem(item, count)

    case RemoveItem(item, count: Int) if cart.items.size == 1 =>
      cart = cart.removeItem(item, count)
      changeContextToEmpty()

    case StartCheckOut() =>
      val checkoutActor = createCheckoutActor()
      sender ! CheckOutStarted(checkoutActor)
      context become inCheckout

    case CartTimerExpired() => changeContextToEmpty()

    case Snap => saveSnapshot(cart)
  }

  def inCheckout: Receive = LoggingReceive {
    case CheckoutClosed() =>
      cart = Cart.empty
      changeContextToEmpty();

    case CheckoutCancelled() => changeContextToNonEmpty()

    case Snap => saveSnapshot(cart)
  }

  def createCheckoutActor(): ActorRef = {
    context.actorOf(Props(new Checkout(customer, self)), "checkout")
  }

  private def changeContextToNonEmpty(): Unit = {
    timers.startSingleTimer(CartTimerKey, CartTimerExpired, 5 second)
    context become nonEmpty
  }

  private def changeContextToEmpty(): Unit = {
    customer ! CartEmpty()
    context become empty
  }
}

///**
//  * @param id: unique item identifier (java.net.URI)
//  */
//case class Item(id: URI, name: String, price: BigDecimal, count: Int)
//case class Cart(items: Map[URI, Item]) {
//  def addItem(it: Item): Cart = {
//    val currentCount = if (items contains it.id) items(it.id).count else 0
//    copy(items = items.updated(it.id, it.copy(count = currentCount + it.count)))
//  }
//  def removeItem(id: Item, cnt: Int) = {
//
//  }
//}
//
//object Cart {
//  val empty = Cart(Map.empty)
//}
//
//class CartManager(var shoppingCart: Cart) extends Actor {
//  def this() = this(Cart.empty)
//
//  def receive = {
//    case AddItem(item, count) => {
//
//    }
//    case RemoveItem(item, count) => {
//
//    }
//  }
//}