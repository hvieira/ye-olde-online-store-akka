package net.hvieira.yeoldeonlinestore.actor

import akka.actor.{Actor, Props}
import net.hvieira.yeoldeonlinestore.actor.Authenticator.{AuthenticateUser, UserAuthenticatedResp}
import net.hvieira.yeoldeonlinestore.actor.OperationResult.OperationResult
import pdi.jwt.{Jwt, JwtAlgorithm, JwtHeader, JwtClaim, JwtOptions}

object Authenticator {
  val props = Props[Authenticator]

  case class AuthenticateUser(val username: String, val password: String)
  case class UserAuthenticatedResp(val result: OperationResult, val userId: String, val authToken: String)
}

class Authenticator extends Actor {

  // TODO to be passed to this actor by props. Check the documentation on dependency injection with Actors
  val secret = "topSekret"

  override def receive: Receive = {
    case AuthenticateUser(username, password) => {

      // TODO change this to use a JSON framework to create these values and change test to assert against the new proper values
      val token = Jwt.encode("""{"alg": "HS256", "typ": "JWT" }""", s"""{"user":"$username"}""", secret, JwtAlgorithm.HS256)

      sender ! UserAuthenticatedResp(OperationResult.OK, "0", token)
    }
  }
}
