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

  def findCurrentUserSession(userId: String): Option[ActorRef] = context.child(userSessionActorName(userId))

  override def receive: Receive = {
    case req@AddItemToUserCart(_, _, user) =>
      findCurrentUserSession(user) match {
        case Some(ref) =>
          ref forward req
        case None =>
          val ref = createUserSession(user)
          ref forward req
      }

    case req@GetUserCart(user) =>
      findCurrentUserSession(user) match {
        case Some(ref) =>
          ref forward req
        case None =>
          val ref = createUserSession(user)
          ref forward req
      }
  }

  private def createUserSession(user: String) = {
    val ref = context.actorOf(Props[UserSession], userSessionActorName(user))
    ref
  }

  private def userSessionActorName(user: String) = {
    s"userSession${user}"
  }
}

private class UserSession extends Actor {

  import context.become

  def handleRequest(state: UserSessionState): Receive = {
    case AddItemToUserCart(item, amount, _) =>
      val result = state.addToCart(item, amount)
      sender ! AddItemToCartResult(OK, result.cart)
      become(handleRequest(result), true)

    case GetUserCart(user) =>
      sender ! UserCart(OK, user, state.cart)
  }

  override def receive: Receive = handleRequest(new UserSessionState())
}


private class UserSessionState(val balance: Double = 0, val cart: Cart = Map()) {

  def addToCart(item: Item, amount: Int): UserSessionState = {
    val currentAmount = cart.getOrElse(item, 0)
    new UserSessionState(balance, cart.updated(item, currentAmount + amount))
  }

}

case class GetUserCart(user: String)

case class UserCart(result: OperationResult, user: String, cart: Cart)

case class AddItemToUserCart(item: Item, amount: Int, userId: String)

case class AddItemToCartResult(result: OperationResult, updatedCart: Cart)

case class FailedReq(reason: String)