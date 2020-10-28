package EShop.lab2

case class Cart(items: Seq[Any]) {
  def contains(item: Any): Boolean = items contains item
  def addItem(item: Any): Cart     = Cart(items :+ item)
  def removeItem(item: Any): Cart  = Cart(items.filterNot(_ == item))
  def size: Int                    = items.size
}

object Cart {
  def empty: Cart = Cart(Seq.empty)
}
