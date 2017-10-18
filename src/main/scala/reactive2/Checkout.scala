package reactive2

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive

object Checkout {

  case class CheckoutStarted()

  case class CheckoutTimerExpired()

  case class DeliveryMethodSelected()

  case class Cancelled()

  case class CartTimerExpired()

  case class PaymentSelected()

  case class PaymentReceived()

  case class PaymentTimerExpired()

}

class Checkout(cart: ActorRef) extends Actor {

  import Checkout._

  def selectingDelivery: Receive = LoggingReceive {
    case Cancelled =>
      cart ! Cart.CheckoutCancelled
      context.stop(self)

    case DeliveryMethodSelected =>
      context become selectingPaymentMethod

    case CheckoutTimerExpired =>
      cart ! Cart.CheckoutCancelled
      context.stop(self)
  }

  def selectingPaymentMethod: Receive = LoggingReceive {
    case Cancelled =>
      cart ! Cart.CheckoutCancelled
      context.stop(self)

    case CheckoutTimerExpired =>
      cart ! Cart.CheckoutCancelled
      context.stop(self)

    case PaymentSelected =>
      context become processingPayment
  }

  def processingPayment: Receive = LoggingReceive {
    case Cancelled =>
      cart ! Cart.CheckoutCancelled
      context.stop(self)

    case PaymentTimerExpired =>
      cart ! Cart.CheckoutCancelled
      context.stop(self)

    case PaymentReceived =>
      cart ! Cart.CheckoutClosed
      context.stop(self)
  }

  def receive: Receive = selectingDelivery
}
