package net.hvieira.yeoldeonlinestore.auth

import net.hvieira.yeoldeonlinestore.actor._
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import spray.json.{DefaultJsonProtocol, _}

import scala.util.{Failure, Success}

object Authentication extends DefaultJsonProtocol {

  private implicit val tokenPayloadFormat = jsonFormat1(TokenPayload)

  private val TOKEN_TTL_SEC = 10 * 60

  def authInfoFromToken(token: String, secret: String): Option[TokenPayload] = {
    val decodedToken = Jwt.decodeRawAll(token, secret, Seq(JwtAlgorithm.HS256))

    decodedToken match {
      case Success((_, claim: String, _)) =>
        Option(claim.parseJson.asJsObject.fields("user"))
          .map(js => js.convertTo[String]) match {
          case Some(user) => Some(TokenPayload(user))
          case _ => None
        }

      case Failure(_) => None
    }
  }

  def generateTokenForUser(username: String, password: String, tokenSecret: String): String = {

    val claim: JwtClaim = JwtClaim(TokenPayload(username).toJson.compactPrint)
      .issuedNow
      .expiresIn(TOKEN_TTL_SEC)
      .startsNow

    Jwt.encode(JwtHeader(JwtAlgorithm.HS256, "JWT"), claim, tokenSecret)
  }

}

case class TokenPayload(user: String)
