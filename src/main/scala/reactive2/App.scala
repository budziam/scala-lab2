package reactive2

import akka.actor._

import scala.concurrent.Await
import scala.concurrent.duration._

object App extends App {
  val system = ActorSystem("app")
  val shopActor = system.actorOf(Props[Customer], "customer")

  shopActor ! Customer.Init()

  Await.result(system.whenTerminated, Duration.Inf)
}
