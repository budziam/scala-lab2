package reactive2

import akka.actor._
import akka.event.LoggingReceive

object PaymentService {

  case class DoPayment()

  case class PaymentConfirmed()

  case class PaymentReceived()

}

class PaymentService(checkout: ActorRef) extends Actor {

  import PaymentService._

  def receive = LoggingReceive {
    case PaymentService.DoPayment() =>
      sender ! PaymentConfirmed()
      checkout ! PaymentReceived()
      context.stop(self)
  }
}