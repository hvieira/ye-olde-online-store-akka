package net.hvieira.yeoldeonlinestore.actor

import akka.actor.{Actor, Props}
import net.hvieira.yeoldeonlinestore.actor.Authenticator.{AuthenticateUser, UserAuthenticatedResp}
import net.hvieira.yeoldeonlinestore.actor.OperationResult.OperationResult

object Authenticator {
  val props = Props[Authenticator]

  case class AuthenticateUser(val username: String, val password: String)
  case class UserAuthenticatedResp(val result: OperationResult, val userId: Option[Int])
}

class Authenticator extends Actor {
  override def receive: Receive = {
    case AuthenticateUser(username, password) => {
      // TODO actual logic
      sender ! UserAuthenticatedResp(OperationResult.OK, Some(0))
    }
  }
}
