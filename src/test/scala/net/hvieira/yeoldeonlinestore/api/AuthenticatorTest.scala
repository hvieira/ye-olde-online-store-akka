package net.hvieira.yeoldeonlinestore.actor

import akka.pattern.ask
import akka.testkit.TestActorRef
import net.hvieira.yeoldeonlinestore.actor.Authenticator.{AuthenticateUser, UserAuthenticatedResp}
import net.hvieira.yeoldeonlinestore.test.ActorUnitTest
import pdi.jwt.{Jwt, JwtAlgorithm}

import scala.util.Success

class AuthenticatorTest extends ActorUnitTest {

  private val goodSecret = "myTopSecret"

  "The authenticator" should {

    val actorRef = TestActorRef(new Authenticator(goodSecret))

    "create JWT tokens on authentication" in {

      val future = actorRef ? AuthenticateUser("someUser", "somePass")
      val Success(result: UserAuthenticatedResp) = future.value.get

      val decodedToken = Jwt.decodeRawAll(result.authToken, goodSecret, Seq(JwtAlgorithm.HS256))

      // TODO this should be improved once a JSON framework is in the project
      decodedToken should matchPattern {
        case Success((_, """{"user":"someUser"}""", _)) =>
      }

    }
  }

}
