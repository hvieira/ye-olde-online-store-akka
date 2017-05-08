package net.hvieira.yeoldeonlinestore.actor

import akka.actor.{Actor, Props}
import net.hvieira.yeoldeonlinestore.actor.Authenticator.{AuthenticateUser, UserAuthenticatedResp}
import net.hvieira.yeoldeonlinestore.actor.OperationResult.OperationResult
import pdi.jwt.{Jwt, JwtAlgorithm}

object Authenticator {
  def props(tokenSecret: String) = Props(new Authenticator(tokenSecret))

  case class AuthenticateUser(val username: String, val password: String)

  case class UserAuthenticatedResp(val result: OperationResult, val userId: String, val authToken: String)

}

class Authenticator(private val tokenSecret: String) extends Actor {

  override def receive: Receive = {
    case AuthenticateUser(username, password) => {

      // TODO change this to use a JSON framework to create these values and change test to assert against the new proper values
      val token =
        Jwt.encode(
          """{"alg": "HS256", "typ": "JWT" }""",
          s"""{"user":"$username"}""",
          tokenSecret,
          JwtAlgorithm.HS256)

      sender ! UserAuthenticatedResp(OperationResult.OK, "0", token)
    }
  }
}
