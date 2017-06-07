package net.hvieira.yeoldeonlinestore.actor

import akka.actor.{Actor, Props}
import net.hvieira.yeoldeonlinestore.actor.OperationResult.OperationResult
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import spray.json._
import Authenticator.TOKEN_TTL_SEC

import scala.util.{Failure, Success}

object Authenticator {
  def props(tokenSecret: String) = Props(new Authenticator(tokenSecret))

  private val TOKEN_TTL_SEC = 10 * 60
}

case class ValidateAuthorizationToken(token: String)

case class AuthorizationTokenValidated(result: OperationResult, tokenPayload: TokenPayload)

case class AuthenticateUser(username: String, password: String)

case class UserAuthenticatedResp(result: OperationResult, userId: String, authToken: String)

case class TokenPayload(user: String)

private object TokenPayloadJsonProtocol extends DefaultJsonProtocol {
  implicit val tokenPayloadFormat = jsonFormat1(TokenPayload)
}

class Authenticator(private val tokenSecret: String) extends Actor {

  import TokenPayloadJsonProtocol._

  override def receive: Receive = {
    case AuthenticateUser(username, password) =>

      val claim: JwtClaim = JwtClaim(TokenPayload(username).toJson.compactPrint)
        .issuedNow
        .expiresIn(TOKEN_TTL_SEC)
        .startsNow

      val token = Jwt.encode(JwtHeader(JwtAlgorithm.HS256, "JWT"), claim, tokenSecret)

      sender ! UserAuthenticatedResp(OperationResult.OK, username, token)

    case ValidateAuthorizationToken(token) =>
      val decodedToken = Jwt.decodeRawAll(token, tokenSecret, Seq(JwtAlgorithm.HS256))

      decodedToken match {
        case Success((_, claim: String, _)) =>
          Option(claim.parseJson.asJsObject.fields("user"))
            .map(js => js.toString()) match {
            case Some(token) => sender ! AuthorizationTokenValidated(OperationResult.OK, TokenPayload(token))
            case _ =>
              println()
              sender ! AuthorizationTokenValidated(OperationResult.NOT_OK, TokenPayload(""))
          }

        case Failure(e) =>
          // TODO use logs
          println("Could not decode Authorization token " + token)
          e.printStackTrace()
          sender ! AuthorizationTokenValidated(OperationResult.NOT_OK, TokenPayload(""))
      }
  }
}
