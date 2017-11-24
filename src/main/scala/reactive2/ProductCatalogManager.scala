package reactive2

import akka.actor.Actor
import reactive2.ProductCatalogMessages.BestMatches

object ProductCatalogMessages {

  case class BestMatches(query: String, n: Int)

}

class ProductCatalogManager(catalog: ProductCatalog) extends Actor {
  override def receive: Receive = {
    case BestMatches(query, n) => sender() ! catalog.bestMatches(query, n)
  }
}