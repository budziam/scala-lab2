package reactive2

import akka.actor._
import com.typesafe.config.ConfigFactory
import reactive2.ProductCatalogMessages.BestMatches

object App extends App {
  val config = ConfigFactory.load()
  val catalogSystem = ActorSystem("productCatalog", config.getConfig("productcatalog").withFallback(config))
  val mainSystem = ActorSystem("main", config.getConfig("main").withFallback(config))
  val shopActor = mainSystem.actorOf(Props[Customer], "customer")

  val currentDirectory = new java.io.File(".").getCanonicalPath

  val lines = scala.io.Source.fromFile("./query_result").getLines

  catalogSystem.actorOf(Props(new ProductCatalogManager(ProductCatalog(lines))), "productCatalog")
  shopActor ! BestMatches("beer", 5)
}