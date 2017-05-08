package net.hvieira.yeoldeonlinestore.actor

import akka.pattern.ask
import akka.testkit.TestActorRef
import net.hvieira.yeoldeonlinestore.actor.Authenticator.{AuthenticateUser, UserAuthenticatedResp}
import net.hvieira.yeoldeonlinestore.test.ActorUnitTest
import pdi.jwt.{Jwt, JwtAlgorithm}

import scala.util.Success

class AuthenticatorTest extends ActorUnitTest {

  "The authenticator" should {
    "create JWT tokens on authentication" in {

      val actorRef = TestActorRef(new Authenticator)

      val future = actorRef ? AuthenticateUser("someUser", "somePass")
      val Success(result: UserAuthenticatedResp) = future.value.get

      // TODO the secret is hardcoded here for now but needs to disappear once the actor receives the configuration
      val decodedToken = Jwt.decodeRawAll(result.authToken, "topSekret", Seq(JwtAlgorithm.HS256))

      // TODO this should be improved
      decodedToken should matchPattern {
        case Success((_, """{"user":"someUser"}""", _)) =>
      }

    }
  }

}
