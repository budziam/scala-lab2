package reactive2

import java.net.URI

case class Item(id: URI, name: String, price: BigDecimal, count: Int)

object Cart {
  val empty = Cart(Map.empty)
}

case class Cart(items: Map[URI, Item]) {
  def addItem(item: Item): Cart = {
    copy(items = items.updated(item.id, item.copy(count = getItemCount(item) + item.count)))
  }

  def removeItem(item: Item, count: Int): Cart = {
    val itemCount = getItemCount(item)
    if (itemCount <= count) {
      return copy(items = items - item.id)
    }

    copy(items = items.updated(item.id, item.copy(count = itemCount - count)))
  }

  private def getItemCount(it: Item): Int = {
    if (items contains it.id) items(it.id).count else 0
  }
}
