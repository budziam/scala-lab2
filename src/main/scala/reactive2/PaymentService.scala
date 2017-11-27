package reactive2

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor._
import akka.event.LoggingReceive
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import reactive2.PaymentService.{PaymentConfirmed, PaymentReceived}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object PaymentService {

  case class DoPayment(paymentSystem: String)

  case class PaymentConfirmed()

  case class PaymentReceived()

}

class PaymentService(checkout: ActorRef) extends Actor with ActorLogging {

  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(loggingEnabled = false) {
    case _: PaymentRejectedException =>
      log.warning("--- TRY AGAIN")
      Restart
    case e =>
      log.error("Unexpected failure: {}", e.getMessage)
      Stop
  }

  def receive = LoggingReceive {
    case PaymentService.DoPayment(paymentSystem: String) =>
      context.actorOf(Props(new PaymentWorker(paymentSystem, sender, checkout)))
  }
}

class PaymentWorker(paymentSystem: String, sender: ActorRef, checkout: ActorRef) extends Actor with ActorLogging {

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  import akka.pattern.pipe
  import context.dispatcher

  val http = Http(context.system)

  override def preStart() = {
    //    val uri = s"http://httpbin.org/anything/$paymentSystem"
    val uri = "http://httpbin.org/status/500"
    log.info("--- REQUESTING")
    http.singleRequest(HttpRequest(uri = uri)).pipeTo(self)
  }

  def receive = {
    case resp@HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        resp.discardEntityBytes()

        sender ! PaymentConfirmed()
        checkout ! PaymentReceived()

        shutdown()
      }

    case resp@HttpResponse(code, _, _, _) =>
      resp.discardEntityBytes()
      log.info("--- PAYMENT REJECTED")
      throw new PaymentRejectedException

  }

  def shutdown() = {
    Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
    context.system.terminate()
  }
}

class PaymentRejectedException extends Exception("Repeat")
