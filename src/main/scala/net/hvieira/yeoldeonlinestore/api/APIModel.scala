package net.hvieira.yeoldeonlinestore.api

final case class Cart(private val items: Map[String, (Int, Double)] = Map()) {

  def addItemsToCart(item: Item, amount: Int): Cart = {
    // TODO include cats lib to use associative monoid   original |+| Map(key -> 1)
    // TODO If you use cats, the import is `cats.syntax.semigroup._` but it is simpler to just import `cats.all._`

    val totalCost = item.cost * amount

    val updatedItemMap = items.updated(item.id, (amount, totalCost)).filter(entry => entry._2._1 > 0)

    Cart(updatedItemMap)
  }

  def itemsToQuantityMap(): Map[String, (Int, Double)] = items

}

final case class Item(id: String, cost: Double)

final case class LoginData(username: String, encryptedPassword: String)

final case class UpdateUserCartRequest(itemId: String, amount: Int)

final case class LoginResult(authToken: String)

final case class StoreFront(items: List[Item])
