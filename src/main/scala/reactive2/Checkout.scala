package reactive2

import akka.actor.{Actor, ActorRef, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._

object Checkout {

  case class CheckoutStarted()

  case class CheckoutTimerExpired()

  case class DeliveryMethodSelected()

  case class Cancelled()

  case class CartTimerExpired()

  case class PaymentSelected()

  case class PaymentReceived()

  case class PaymentTimerExpired()

  case object CheckoutTimerKey

  case object PaymentTimerKey

}

class Checkout(cart: ActorRef) extends Actor with Timers {

  import Checkout._

  timers.startSingleTimer(CheckoutTimerKey, CheckoutTimerExpired, 5 second)

  def cancellCheckout(): Unit = {
    cart ! CheckoutCancelled
    context.stop(self)
  }

  def selectingDelivery: Receive = LoggingReceive {
    case Cancelled => cancellCheckout()
    case CheckoutTimerExpired => cancellCheckout()
    case DeliveryMethodSelected => context become selectingPaymentMethod
  }

  def selectingPaymentMethod: Receive = LoggingReceive {
    case Cancelled => cancellCheckout()
    case CheckoutTimerExpired => cancellCheckout()
    case PaymentSelected =>
      timers.startSingleTimer(PaymentTimerKey, PaymentTimerExpired, 5 second)
      context become processingPayment
  }

  def processingPayment: Receive = LoggingReceive {
    case Cancelled => cancellCheckout()
    case PaymentTimerExpired => cancellCheckout()

    case PaymentReceived =>
      cart ! CheckoutClosed
      context.stop(self)
  }

  def receive: Receive = selectingDelivery
}
