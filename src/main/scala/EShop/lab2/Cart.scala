package EShop.lab2

case class Cart(items: Seq[Any]) {
  def contains(item: Any): Boolean = items.contains(item)

  def addItem(item: Any): Cart = copy(items :+ item)

  def removeItem(item: Any): Cart = copy(items.filterNot((x: Any) => x.equals(item)))

  def size: Int = items.size
}


object Cart {
  def empty: Cart = Cart(Seq.empty)
}
