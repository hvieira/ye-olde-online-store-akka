package net.hvieira.yeoldeonlinestore.api

final case class Item(id: String, cost: Double)

final case class ItemAndQuantity(item: Item, amount: Int)

final case class Cart(items: Map[String, ItemAndQuantity] = Map()) {

  def addItemsToCart(item: Item, amount: Int): Cart = {
    // TODO include cats lib to use associative monoid   original |+| Map(key -> 1)
    // TODO If you use cats, the import is `cats.syntax.semigroup._` but it is simpler to just import `cats.all._`

    val updatedItemMap = items.updated(item.id, ItemAndQuantity(item, amount)).filter(entry => entry._2.amount > 0)

    Cart(updatedItemMap)
  }
}

final case class LoginData(username: String, encryptedPassword: String)

final case class UpdateUserCartRequest(itemId: String, amount: Int)

final case class LoginResult(authToken: String)

final case class StoreFront(items: List[Item])
