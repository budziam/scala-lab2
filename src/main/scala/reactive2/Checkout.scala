package reactive2

import akka.actor.{ActorRef, FSM}

import scala.concurrent.duration._

case object DeliveryMethodSelected

case object Cancelled

case object CheckoutTimerExpired

case object PaymentTimerExpired

case object PaymentSelected

case object PaymentReceived


sealed trait CheckoutState

case object ProcessingPayment extends CheckoutState

case object SelectingPaymentMethod extends CheckoutState

case object SelectingDelivery extends CheckoutState


sealed trait CheckoutData

case object CheckoutUninitialized extends CheckoutData

case class CheckoutInfo() extends CheckoutData


class Checkout(cart: ActorRef) extends FSM[CheckoutState, CheckoutData] {

  startWith(SelectingDelivery, CheckoutUninitialized)

  when(SelectingDelivery) {
    case Event(DeliveryMethodSelected, CheckoutUninitialized) =>
      goto(SelectingPaymentMethod) using CheckoutInfo()
  }

  when(SelectingPaymentMethod) {
    case Event(PaymentSelected, CheckoutInfo()) =>
      goto(ProcessingPayment) using CheckoutInfo()
  }

  when(ProcessingPayment) {
    case Event(PaymentReceived, _: CheckoutInfo) =>
      cart ! CheckoutClosed
      stop()

    case Event(PaymentTimerExpired, _) =>
      cart ! CheckoutCancelled
      stop()

  }

  whenUnhandled {
    case Event(Cancelled, _) =>
      cart ! CheckoutCancelled
      stop()

    case Event(CheckoutTimerExpired, _) =>
      cart ! CheckoutCancelled
      stop()
  }

  onTransition {
    case _ -> SelectingDelivery => setTimer("checkoutTimer", CheckoutTimerExpired, 3 seconds)
    case SelectingPaymentMethod -> ProcessingPayment =>
      cancelTimer("checkoutTimer")
      setTimer("paymentTimer", PaymentTimerExpired, 3 seconds)
  }

  initialize()
}
