package reactive2

import java.net.URI

import scala.collection.Iterator

case class ProductCatalog(products: Iterator[String]) {

  def bestMatches(query: String, n: Int = 10): List[Item] = {
    this.getProductCatalog
      .sortBy(-score(_, query))
      .take(n)
      .toList
  }

  private def getProductCatalog: Stream[Item] = {
    this.products.toStream.tail
      .map(_.split(",").map(_.replace("\"", "")))
      .filter(_.length > 2)
      .map(x => {
          val name = x(1) + " " + x(2).trim
          val uri = URI.create(x(0))
          val price = x(2).length
          val count = name.length
          Item(uri, name, price, count)
      })
      .filterNot(x => x.name contains "NULL")
  }

  private def score(item: Item, query: String): Int = {
    val itemParts = item.name.split(" ").map(_.toLowerCase)
    val queryParts = query.split(" ").map(_.toLowerCase)
    itemParts.map(queryParts contains _).map(if (_) 1 else 0).sum
  }
}
