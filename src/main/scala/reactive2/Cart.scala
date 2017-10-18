package reactive2

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.LoggingReceive

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

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

  case object CartTimerKey

}

class Cart extends Actor with Timers {

  import Cart._

  val items: ListBuffer[Item] = ListBuffer[Item]()
  var checkoutActor: ActorRef = _

  def changeContextToNonEmpty(): Unit = {
    context become nonEmpty
    timers.startSingleTimer(CartTimerKey, CartTimerExpired, 5 second)
  }

  def empty: Receive = LoggingReceive {
    case ItemAdded(item) =>
      items += item
      changeContextToNonEmpty()
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
      changeContextToNonEmpty()
  }

  def receive: Receive = empty
}