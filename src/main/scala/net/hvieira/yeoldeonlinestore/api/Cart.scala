package net.hvieira.yeoldeonlinestore.api

// TODO add tests for this
final case class Cart(private val items: Map[Item, Int] = Map()) {

  def addItemsToCart(item: Item, amount: Int): Cart = {
    // TODO include cats lib to use associative monoid   original |+| Map(key -> 1)
    // TODO If you use cats, the import is `cats.syntax.semigroup._` but it is simpler to just import `cats.all._`
    val originalQuantity = items.getOrElse(item, 0)
    Cart(items.updated(item, originalQuantity + amount))
  }

  def itemsToQuantityMap(): Map[Item, Int] = items

}

case class JsonTestCaseClass(stuff: Map[Item, Int])
