package reactive2

import akka.actor._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object App2 extends App {
  val system = ActorSystem("app")
  val shopActor = system.actorOf(Props[Customer], "customer")

  shopActor ! Customer.Purchase()

  Await.result(system.whenTerminated, Duration.Inf)
}