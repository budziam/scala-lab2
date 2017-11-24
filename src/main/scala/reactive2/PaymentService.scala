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
    case _: RepeatPaymentException =>
      log.warning("REPEAT PAYMENT")
      Restart
    case e =>
      log.error("Unexpected failure: {}", e.getMessage)
      Stop
  }

  var worker: ActorRef = _

  def receive = LoggingReceive {
    case PaymentService.DoPayment(paymentSystem: String) =>
      worker = context.actorOf(Props(new PaymentWorker(paymentSystem, sender, checkout)))
    //      context.stop(self)
  }
}

class PaymentWorker(paymentSystem: String, sender: ActorRef, checkout: ActorRef) extends Actor {

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  import akka.pattern.pipe
  import context.dispatcher

  val http = Http(context.system)

  override def preStart() = {
    val uri = s"http://httpbin.org/anything/$paymentSystem"
    http.singleRequest(HttpRequest(uri = uri)).pipeTo(self)
  }

  def receive = {
    case resp@HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
//        println("Got response, body: " + body.utf8String)
        resp.discardEntityBytes()

        sender ! PaymentConfirmed()
        checkout ! PaymentReceived()

        shutdown()
      }

    case resp@HttpResponse(code, _, _, _) =>
//      println("Request failed, response code: " + code)
      resp.discardEntityBytes()
      throw new RepeatPaymentException
      shutdown()

  }

  def shutdown() = {
    Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
    context.system.terminate()
  }
}

class RepeatPaymentException extends Exception("Repeat")
