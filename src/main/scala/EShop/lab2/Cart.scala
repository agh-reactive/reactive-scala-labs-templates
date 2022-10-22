package EShop.lab2

case class Cart(items: Seq[Any]) {
  def contains(item: Any): Boolean = this.items.contains(item)
  def addItem(item: Any): Cart     = Cart(this.items :+ item)
  def removeItem(item: Any): Cart  = Cart(this.items diff List(item))
  def size: Int                    = this.items.length
}

object Cart {
  def empty: Cart = Cart(Seq())
}
