package reactive2

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive

import scala.collection.mutable.ListBuffer

class Item(name: String, price: Int) {
  //
}

object Cart {

  case class ItemAdded(item: Item)

  case class ItemRemoved(item: Item)

  case class CheckoutStarted()

  case class CheckoutCancelled()

  case class CheckoutClosed()

  case class CartTimerExpired()

  case class CheckoutCreated(checkout: ActorRef)

}

class Cart extends Actor {

  import Cart._

  val items: ListBuffer[Item] = ListBuffer[Item]()
  var checkoutActor: ActorRef = _

  def empty: Receive = LoggingReceive {
    case ItemAdded(item) =>
      items += item
      context become nonEmpty
  }

  def nonEmpty: Receive = LoggingReceive {
    case ItemAdded(item) =>
      items += item

    case ItemRemoved(item) if items.size > 1 =>
      items -= item

    case ItemRemoved(item) if items.size == 1 =>
      items -= item
      context become empty

    case CheckoutStarted =>
      checkoutActor = context.actorOf(Props(new Checkout(self)), "checkout")
      sender ! CheckoutCreated(checkoutActor)
      context become inCheckout

    case CartTimerExpired =>
      context become empty
  }

  def inCheckout: Receive = LoggingReceive {
    case CheckoutClosed =>
      items.clear()
      context become empty

    case CheckoutCancelled =>
      context become nonEmpty
  }

  def receive: Receive = empty
}