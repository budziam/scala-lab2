package reactive2

import akka.actor.{ActorRef, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence._

import scala.concurrent.duration._

object CartManager {

  case object StartCheckOut

  case class CheckOutStarted(checkout: ActorRef)

  case object CheckoutCancelled

  case object CheckoutClosed

  case object CartTimerExpired

  case object CartEmpty

  sealed trait CartState

  case object Empty extends CartState

  case class NonEmpty(timestamp: Long) extends CartState

  case object InCheckout extends CartState

  sealed trait Event

  case class AddItem(item: Item) extends Event

  case class RemoveItem(item: Item, count: Int) extends Event

  case object ClearCart extends Event

  private case class ChangeState(state: CartState) extends Event

}

class CartManager(customer: ActorRef, id: String, var cart: Cart) extends PersistentActor with Timers {

  import CartManager._

  override def persistenceId: String = id

  val cartTimeout: FiniteDuration = 60 seconds

  def this(customer: ActorRef) = {
    this(customer, "cart-manager-1", Cart.empty)
  }

  def this(customer: ActorRef, id: String) = {
    this(customer, id, Cart.empty)
  }

  private def startTimer(timestamp: Long, time: FiniteDuration): Unit = {
    timers.startSingleTimer("cart-manager-timer-" + timestamp, CartTimerExpired, time)
  }

  private def cancelTimer(): Unit = {
    timers.cancelAll()
  }

  def empty: Receive = LoggingReceive {
    case AddItem(item: Item) =>
      persist(AddItem(item: Item)) { event =>
        updateState(event)
        changeContextToNonEmpty()
      }
  }

  def nonEmpty: Receive = LoggingReceive {
    case ClearCart => changeContextToEmpty()

    case AddItem(item) =>
      persist(AddItem(item)) { event =>
        updateState(event)
      }

    case RemoveItem(item, count) if cart.items contains item.id =>
      persist(RemoveItem(item, count)) { event =>
        updateState(event)
        if (cart.items.isEmpty) changeContextToEmpty()
      }

    case StartCheckOut =>
      val checkoutActor = createCheckoutActor()
      sender ! CheckOutStarted(checkoutActor)
      persist(ChangeState(InCheckout)) { event => updateState(event) }

    case CartTimerExpired => changeContextToEmpty()
  }

  def inCheckout: Receive = LoggingReceive {
    case CheckoutClosed => changeContextToEmpty();
    case CheckoutCancelled => changeContextToNonEmpty()
  }

  private def updateState(event: Event): Unit = event match {
    case ClearCart => cart = Cart.empty
    case AddItem(item) => cart = cart.addItem(item)
    case RemoveItem(item, count) => cart = cart.removeItem(item, count)
    case ChangeState(state) => state match {
      case Empty =>
        cancelTimer()
        context become empty
      case NonEmpty(timestamp) =>
        val now = System.currentTimeMillis()
        val diff = Math.max((now - timestamp) / 1000.0, 0)
        startTimer(timestamp, cartTimeout - diff.seconds)
        context become nonEmpty
      case InCheckout =>
        cancelTimer()
        context become inCheckout
    }
  }

  def createCheckoutActor(): ActorRef = {
    context.actorOf(Props(new Checkout(customer, self)), "checkout")
  }

  private def changeContextToEmpty(): Unit = {
    persist(ClearCart) { event =>
      updateState(event)
      customer ! CartEmpty
      persist(ChangeState(Empty)) { event => updateState(event) }
    }
  }

  private def changeContextToNonEmpty(): Unit = {
    val now = System.currentTimeMillis()
    persist(ChangeState(NonEmpty(now))) { event => updateState(event) }
  }

  override def receiveCommand: Receive = empty

  override def receiveRecover: Receive = LoggingReceive {
    case event: Event => updateState(event)
    case SnapshotOffer(_, snapshot: Cart) => cart = snapshot
  }
}
