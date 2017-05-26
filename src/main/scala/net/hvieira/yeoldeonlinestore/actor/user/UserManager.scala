package net.hvieira.yeoldeonlinestore.actor.user

import akka.actor.{Actor, ActorRef, Props}
import net.hvieira.yeoldeonlinestore.actor.OperationResult._
import net.hvieira.yeoldeonlinestore.actor.user.UserManager.Cart
import net.hvieira.yeoldeonlinestore.api.Item

object UserManager {
  def props(): Props = Props[UserManager]

  type Cart = Map[Item, Int]
}

class UserManager extends Actor {

  def findCurrentUserSession(userId: String): Option[ActorRef] = context.child(s"userSession_${userId}")

  override def receive: Receive = {
    case req@AddItemToUserCart(_, _, userId) =>
      findCurrentUserSession(userId) match {
        case Some(ref) =>
          ref forward req
        case None =>
          val ref = context.actorOf(Props[UserSession], s"userSession_${userId}")
          ref forward req
      }
  }

}

private class UserSession extends Actor {

  import context.become

  def handleRequest(state: UserSessionState): Receive = {
    case AddItemToUserCart(item, amount, _) =>
      val result = state.addToCart(item, amount)
      sender ! AddItemToCartResult(OK, result.cart)
      become(handleRequest(result), true)
  }

  override def receive: Receive = handleRequest(new UserSessionState())
}


private class UserSessionState(val balance: Double = 0, val cart: Cart = Map()) {

  def addToCart(item: Item, amount: Int): UserSessionState = {
    val currentAmount = cart.getOrElse(item, 0)
    new UserSessionState(balance, cart.updated(item, currentAmount + amount))
  }

}

case class AddItemToUserCart(item: Item, amount: Int, userId: String)

case class AddItemToCartResult(result: OperationResult, updatedCart: Cart)

case class FailedReq(reason: String)