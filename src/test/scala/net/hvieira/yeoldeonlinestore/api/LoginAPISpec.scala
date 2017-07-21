package net.hvieira.yeoldeonlinestore.api

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import net.hvieira.yeoldeonlinestore.auth.Authentication
import net.hvieira.yeoldeonlinestore.test.ServiceIntegrationTest

class LoginAPISpec extends ServiceIntegrationTest {

  private val tokenSecret = "testSecret"
  private val tokenGenerator = Authentication.tokenGenerator(tokenSecret)
  private val route = Route.seal(new LoginAPI(tokenGenerator).route)

  "The login API" should {

    "login users sucessfully and provide a JWT token for further authenticated requests" in {
      val authToken = authenticateUserAndGetToken("Johnny", "Bravo", tokenSecret)
      authToken should fullyMatch regex "(.+)\\.(.+)\\.(.+)"
    }

    "return unsupported media type if content type is unexpected" in {

      val request: HttpRequest = Post("/login",
        HttpEntity(
          ContentType(MediaTypes.`application/javascript`, HttpCharsets.`UTF-8`),
          "some l33t hax code"))

      request ~> route ~> check {
        status shouldBe UnsupportedMediaType
        handled shouldBe true
      }
    }

    "return bad request if expected login data is missing or empty" in {

      val request: HttpRequest = Post("/login",
        HttpEntity(
          ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),
          "username=&password="))

      request ~> route ~> check {
        status shouldBe BadRequest
        handled shouldBe true
      }
    }

  }

}
