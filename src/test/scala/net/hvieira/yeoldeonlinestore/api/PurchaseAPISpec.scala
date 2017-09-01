package net.hvieira.yeoldeonlinestore.api

import akka.actor.{ActorSystem, Inbox}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import net.hvieira.yeoldeonlinestore.actor.user.{UpdateCart, UserManager}
import net.hvieira.yeoldeonlinestore.auth.Authentication
import net.hvieira.yeoldeonlinestore.test.ServiceIntegrationTest
import akka.pattern.ask
import scala.concurrent.duration._

class PurchaseAPISpec extends ServiceIntegrationTest {

  private val testActorSystem = ActorSystem("test-system")
  private val tokenSecret = "testSecret"

  private val userManRef = testActorSystem.actorOf(UserManager.props())

  private val route = Route.seal(new PurchaseAPI(userManRef, Authentication.requestTokenAuthenticator(tokenSecret)).route)

  private val expectedEndpoint = "/purchase"

  "the purchase API" should {

    "not allow requests with invalid credentials" in {

      val token = "im.leet.hax"
      val authHeader = Authorization(OAuth2BearerToken(token))

      val request: HttpRequest = Post(expectedEndpoint).addHeader(authHeader)

      request ~> route ~> check {
        status shouldBe Unauthorized
        handled shouldBe true
      }
    }

    "not allow requests that have no Authorization token" in {

      val request: HttpRequest = Post(expectedEndpoint)

      request ~> route ~> check {
        status shouldBe Unauthorized
        handled shouldBe true
      }
    }

    "bad request when purchasing an empty cart" in {

      val token = authenticateUserAndGetToken("purchaseEmptyCartUser", "userSekret", tokenSecret)
      val authHeader = Authorization(OAuth2BearerToken(token))

      val request: HttpRequest = Post(expectedEndpoint).addHeader(authHeader)

      request ~> route ~> check {
        status shouldBe BadRequest
        handled shouldBe true
      }
    }

    "allow authenticated requests to purchase the items in the user cart" in {

      val token = authenticateUserAndGetToken("spender", "userSekret", tokenSecret)
      val authHeader = Authorization(OAuth2BearerToken(token))

      implicit val timeout = Timeout(1 second)
      userManRef ? UpdateCart(Item("id", 1.2), 7, "spender")

      val request: HttpRequest = Post(expectedEndpoint).addHeader(authHeader)

      request ~> route ~> check {
        status shouldBe OK
        handled shouldBe true

        val result = entityAs[SuccessfulPurchase]
        val cart = result.cart
        cart.items should contain ("id" -> ItemAndQuantity(Item("id", 1.2), 7))
      }
    }

  }

}
