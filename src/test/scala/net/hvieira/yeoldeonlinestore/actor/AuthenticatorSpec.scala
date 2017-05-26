package net.hvieira.yeoldeonlinestore.actor

import akka.pattern.ask
import akka.testkit.TestActorRef
import net.hvieira.yeoldeonlinestore.test.ActorUnitTest
import org.scalatest.Inside._
import pdi.jwt.{Jwt, JwtAlgorithm}
import spray.json._

import scala.util.Success

class AuthenticatorSpec extends ActorUnitTest {

  private val goodSecret = "myTopSecret"

  "The authenticator" should {

    val actorRef = TestActorRef(new Authenticator(goodSecret))

    "create JWT tokens on authentication" in {

      val future = actorRef ? AuthenticateUser("someUser", "somePass")
      val Success(result: UserAuthenticatedResp) = future.value.get

      Jwt.isValid(result.authToken, goodSecret, Seq(JwtAlgorithm.HS256)) shouldBe true

      val decodedToken = Jwt.decodeRawAll(result.authToken, goodSecret, Seq(JwtAlgorithm.HS256))

      inside(decodedToken) {
        case Success((_, claim: String, _)) => {
          val jsonObj = claim.parseJson.asJsObject.fields should contain("user" -> JsString("someUser"))
        }
      }

    }
  }

}
