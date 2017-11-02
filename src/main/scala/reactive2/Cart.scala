package reactive2

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.LoggingReceive

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

class Item(name: String, price: Int) {
  //
}

object Cart {

  case class AddItem(item: Item)

  case class RemoveItem(item: Item)

  case class StartCheckOut()

  case class CheckoutCancelled()

  case class CheckoutClosed()

  case class CartTimerExpired()

  case class CheckOutStarted(checkout: ActorRef)

  case class CartEmpty()

  case object CartTimerKey

}

class Cart(customer: ActorRef) extends Actor with Timers {

  import Cart._

  val items: ListBuffer[Item] = ListBuffer[Item]()

  def receive: Receive = empty

  def empty: Receive = LoggingReceive {
    case AddItem(item) =>
      items += item
      changeContextToNonEmpty()
  }

  def nonEmpty: Receive = LoggingReceive {
    case AddItem(item) =>
      items += item

    case RemoveItem(item) if items.size > 1 =>
      items -= item

    case RemoveItem(item) if items.size == 1 =>
      items -= item
      changeContextToEmpty()

    case StartCheckOut() =>
      val checkoutActor = context.actorOf(Props(new Checkout(customer, self)), "checkout")
      sender ! CheckOutStarted(checkoutActor)
      context become inCheckout

    case CartTimerExpired() =>
      changeContextToEmpty()
  }

  def inCheckout: Receive = LoggingReceive {
    case CheckoutClosed() =>
      items.clear()
      changeContextToEmpty();

    case CheckoutCancelled() =>
      changeContextToNonEmpty()
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