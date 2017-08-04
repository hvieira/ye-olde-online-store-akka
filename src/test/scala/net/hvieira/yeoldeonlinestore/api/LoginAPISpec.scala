package net.hvieira.yeoldeonlinestore.api

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import net.hvieira.yeoldeonlinestore.auth.{Authentication, UserAuthenticator}
import net.hvieira.yeoldeonlinestore.test.ServiceIntegrationTest
import net.hvieira.yeoldeonlinestore.user.User

class LoginAPISpec extends ServiceIntegrationTest {

  private val tokenSecret = "testSecret"
  private val tokenGenerator = Authentication.tokenGenerator(tokenSecret)
  private val userAuthenticator = new UserAuthenticator {
    override def authenticate(username: String, password: String): Option[User] = {
      if ("username".equals("Johnny") && password.equals("Bravo"))
        Some(new User("id1", "Johnny"))
      else
        None
    }
  }

  private val route = Route.seal(new LoginAPI(tokenGenerator, userAuthenticator).route)

  "The login API" should {

    "login users successfully and provide a JWT token for further authenticated requests" in {
      val authToken = authenticateUserAndGetToken("Johnny", "Bravo", tokenSecret)
      authToken should fullyMatch regex "(.+)\\.(.+)\\.(.+)"
    }

    "return Unauthorized error if authentication fails" in {
      val badPasswordReq: HttpRequest = Post("/login",
        HttpEntity(
          ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),
          "username=Johnny&password=Bravoooo"))

      badPasswordReq ~> route ~> check {
        status shouldBe Unauthorized
        handled shouldBe true
      }
    }

    "return Unauthorized error if an user does not exist with the given credentials" in {
      val nonExistingUserReq: HttpRequest = Post("/login",
        HttpEntity(
          ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),
          "username=John&password=Bravo"))

      nonExistingUserReq ~> route ~> check {
        status shouldBe Unauthorized
        handled shouldBe true
      }
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
