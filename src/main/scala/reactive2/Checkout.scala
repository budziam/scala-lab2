package reactive2

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._

object Checkout {

  case class CheckoutStarted()

  case class CheckoutTimerExpired()

  case class DeliveryMethodSelected()

  case class Cancelled()

  case class CartTimerExpired()

  case class PaymentSelected()

  case class PaymentTimerExpired()

  case class PaymentServiceStarted(paymentService: ActorRef)

  case object CheckoutTimerKey

  case object PaymentTimerKey

}

class Checkout(customer: ActorRef, cart: ActorRef) extends Actor with Timers {

  import Checkout._

  timers.startSingleTimer(CheckoutTimerKey, CheckoutTimerExpired, 5 second)

  def cancellCheckout(): Unit = {
    cart ! CartManager.CheckoutCancelled()
    context.stop(self)
  }

  def selectingDelivery: Receive = LoggingReceive {
    case Cancelled() => cancellCheckout()
    case CheckoutTimerExpired() => cancellCheckout()
    case DeliveryMethodSelected() => context become selectingPaymentMethod
  }

  def selectingPaymentMethod: Receive = LoggingReceive {
    case Cancelled() => cancellCheckout()
    case CheckoutTimerExpired() => cancellCheckout()
    case PaymentSelected() =>
      timers.startSingleTimer(PaymentTimerKey, PaymentTimerExpired, 5 second)

      val paymentService = context.actorOf(Props(new PaymentService(self)), "paymentService")
      customer ! PaymentServiceStarted(paymentService)

      context become processingPayment
  }

  def processingPayment: Receive = LoggingReceive {
    case Cancelled() => cancellCheckout()
    case PaymentTimerExpired() => cancellCheckout()

    case PaymentService.PaymentReceived() =>
      customer ! CartManager.CheckoutClosed()
      cart ! CartManager.CheckoutClosed()
      context.stop(self)
  }

  def receive: Receive = selectingDelivery
}
