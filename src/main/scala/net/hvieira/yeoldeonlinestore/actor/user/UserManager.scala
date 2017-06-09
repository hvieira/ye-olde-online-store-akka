package net.hvieira.yeoldeonlinestore.actor.user

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import net.hvieira.yeoldeonlinestore.actor.OperationResult
import net.hvieira.yeoldeonlinestore.actor.OperationResult._
import net.hvieira.yeoldeonlinestore.api.{Cart, Item, UpdateUserCartRequest}

object UserManager {
  def props(): Props = Props[UserManager]
}

class UserManager extends Actor {

  def findCurrentUserSession(userId: String): Option[ActorRef] = context.child(userSessionActorName(userId))

  override def receive: Receive = {
    case req@UpdateCart(_, _, user) =>
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
    context.actorOf(Props[UserSession], userSessionActorName(user))
  }

  private def userSessionActorName(user: String) = {
    s"userSession${user}"
  }
}

private class UserSession extends Actor with ActorLogging {

  import context.become

  def handleRequest(state: UserSessionState): Receive = {
    case UpdateCart(item, amount, user) =>
      val result = state.addToCart(item, amount)
      log.info("Updated cart is {}", result.cart)
      sender ! UserCart(OperationResult.OK, user, result.cart)
      become(handleRequest(result), true)

    case GetUserCart(user) =>
      sender ! UserCart(OperationResult.OK, user, state.cart)

  }

  override def receive: Receive = handleRequest(new UserSessionState())
}


private class UserSessionState(val balance: Double = 0, val cart: Cart = Cart(Map())) {

  def addToCart(item: Item, amount: Int): UserSessionState =
    new UserSessionState(balance, cart.addItemsToCart(item, amount))
}

case class GetUserCart(user: String)

case class UserCart(result: OperationResult, user: String, cart: Cart)

case class UpdateCart(item: Item, amount: Int, userId: String)