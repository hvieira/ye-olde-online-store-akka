package net.hvieira.yeoldeonlinestore.actor

import akka.actor.{Actor, Props}
import net.hvieira.yeoldeonlinestore.actor.OperationResult.OperationResult
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import spray.json._
import Authenticator.TOKEN_TTL_SEC

object Authenticator {
  def props(tokenSecret: String) = Props(new Authenticator(tokenSecret))

  private val TOKEN_TTL_SEC = 10 * 60
}

case class AuthenticateUser(username: String, password: String)

case class UserAuthenticatedResp(result: OperationResult, userId: String, authToken: String)

case class TokenPayload(user: String)

private object TokenPayloadJsonProtocol extends DefaultJsonProtocol {
  implicit val tokenPayloadFormat = jsonFormat1(TokenPayload)
}

class Authenticator(private val tokenSecret: String) extends Actor {

  import TokenPayloadJsonProtocol._

  override def receive: Receive = {
    case AuthenticateUser(username, password) => {

      val claim: JwtClaim = JwtClaim(TokenPayload(username).toJson.compactPrint)
        .issuedNow
        .expiresIn(TOKEN_TTL_SEC)
        .startsNow

      val token = Jwt.encode(JwtHeader(JwtAlgorithm.HS256, "JWT"), claim, tokenSecret)

      sender ! UserAuthenticatedResp(OperationResult.OK, "0", token)
    }
  }
}
