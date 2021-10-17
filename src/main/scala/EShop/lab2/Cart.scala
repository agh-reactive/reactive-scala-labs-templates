package EShop.lab2

case class Cart(items: Seq[Any]) {
  def contains(item: Any): Boolean = items.contains(item)
  def addItem(item: Any): Cart     = Cart(item +: items)
  def removeItem(item: Any): Cart  = Cart(items diff List(item))
  def size: Int                    = items.length
}

object Cart {
  def empty: Cart = Cart(List())
}
