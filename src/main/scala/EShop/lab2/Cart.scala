package EShop.lab2

case class Cart(items: Seq[Any]) {
  def contains(item: Any): Boolean = ???
  def addItem(item: Any): Cart     = ???
  def removeItem(item: Any): Cart  = ???
  def size: Int                    = ???
}

object Cart {
  def empty: Cart = ???
}
