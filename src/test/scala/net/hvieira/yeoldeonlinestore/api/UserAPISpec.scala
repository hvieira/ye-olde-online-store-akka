package net.hvieira.yeoldeonlinestore.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Route
import net.hvieira.yeoldeonlinestore.actor.store.StoreManager
import net.hvieira.yeoldeonlinestore.actor.user.UserManager
import net.hvieira.yeoldeonlinestore.auth.Authentication
import net.hvieira.yeoldeonlinestore.test.ServiceIntegrationTest

class UserAPISpec extends ServiceIntegrationTest {

  // TODO probably want to change this to use the akka testkit probe actors and such or with DI
  private val testActorSystem = ActorSystem("test-system")
  private val tokenSecret = "testSecret"

  private val itemsProvider = () => List(
    Item("anItem", 1.5),
    Item("anotherItem", 2.709)
  )

  private val storeManRef = testActorSystem.actorOf(StoreManager.props(3, itemsProvider))
  private val userManRef = testActorSystem.actorOf(UserManager.props())

  private val route = Route.seal(new UserAPI(userManRef, storeManRef, Authentication.requestTokenAuthenticator(tokenSecret)).route)

  "the user API" should {

    "not allow requests with invalid credentials" in {

      val token = "im.leet.hax"
      val authHeader = Authorization(OAuth2BearerToken(token))

      val request: HttpRequest = Get("/user/cart").addHeader(authHeader)

      request ~> route ~> check {
        status shouldBe Unauthorized
        handled shouldBe true
      }
    }

    "not allow requests that have no Authorization token" in {

      val request: HttpRequest = Get("/user/cart")

      request ~> route ~> check {
        status shouldBe Unauthorized
        handled shouldBe true
      }
    }

    "define empty cart when an user session is started" in {

      val token = authenticateUserAndGetToken("sessionStartEmptyCartUser", "userSekret", tokenSecret)
      val authHeader = Authorization(OAuth2BearerToken(token))

      val request: HttpRequest = Get("/user/cart").addHeader(authHeader)

      request ~> route ~> check {
        status shouldBe OK
        handled shouldBe true

        val cart = entityAs[Cart]
        cart.items shouldBe empty
      }
    }

    "allow authenticated requests to put items in user cart" in {

      val token = authenticateUserAndGetToken("user1", "userSekret", tokenSecret)
      val authHeader = Authorization(OAuth2BearerToken(token))

      // place the first item in the cart
      {
        val request: HttpRequest = Put("/user/cart", UpdateUserCartRequest("anItem", 2)).addHeader(authHeader)

        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true

          val cart = entityAs[Cart]
          cart.items should contain("anItem" -> ItemAndQuantity(Item("anItem", 1.5), 2))
        }
      }

      // add another item
      {
        val request: HttpRequest = Put("/user/cart", UpdateUserCartRequest("anotherItem", 5)).addHeader(authHeader)

        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true

          val cart = entityAs[Cart]
          cart.items should contain allOf(
            "anItem" -> ItemAndQuantity(Item("anItem", 1.5), 2),
            "anotherItem" -> ItemAndQuantity(Item("anotherItem", 2.709), 5)
          )
        }
      }
    }

    "allow authenticated requests to update item quantity in user cart" in {

      val token = authenticateUserAndGetToken("user2", "userSekret", tokenSecret)
      val authHeader = Authorization(OAuth2BearerToken(token))

      // place the first item in the cart
      {
        val request: HttpRequest = Put("/user/cart", UpdateUserCartRequest("anItem", 2)).addHeader(authHeader)

        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true

          val cart = entityAs[Cart]
          cart.items should contain("anItem" -> ItemAndQuantity(Item("anItem", 1.5), 2))
        }
      }

      // update an item quantity
      {
        val request: HttpRequest = Put("/user/cart", UpdateUserCartRequest("anItem", 5)).addHeader(authHeader)

        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true

          val cart = entityAs[Cart]
          cart.items should contain("anItem" -> ItemAndQuantity(Item("anItem", 1.5), 5))
        }
      }

      {
        val request: HttpRequest = Put("/user/cart", UpdateUserCartRequest("anItem", 3)).addHeader(authHeader)

        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true

          val cart = entityAs[Cart]
          cart.items should contain("anItem" -> ItemAndQuantity(Item("anItem", 1.5), 3))
        }
      }
    }

    "allow authenticated requests to remove items from user cart" in {

      val token = authenticateUserAndGetToken("user3", "userSekret", tokenSecret)
      val authHeader = Authorization(OAuth2BearerToken(token))

      // place the first item in the cart
      {
        val request: HttpRequest = Put("/user/cart", UpdateUserCartRequest("anItem", 2)).addHeader(authHeader)

        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true

          val cart = entityAs[Cart]
          cart.items should contain("anItem" -> ItemAndQuantity(Item("anItem", 1.5), 2))
        }
      }

      // remove the item from cart
      {
        val request: HttpRequest = Put("/user/cart", UpdateUserCartRequest("anItem", 0)).addHeader(authHeader)

        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true

          val cart = entityAs[Cart]
          cart.items shouldBe empty
        }
      }
    }

    "allow authenticated requests to retrieve user cart" in {

      val token = authenticateUserAndGetToken("getMyCartUser", "userSekret", tokenSecret)
      val authHeader = Authorization(OAuth2BearerToken(token))

      {
        val request: HttpRequest = Put("/user/cart", UpdateUserCartRequest("anItem", 17)).addHeader(authHeader)

        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true
        }
      }

      {
        val request: HttpRequest = Get("/user/cart").addHeader(authHeader)
        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true

          val cart = entityAs[Cart]
          cart.items should contain("anItem" -> ItemAndQuantity(Item("anItem", 1.5), 17))
        }
      }
    }

    "not allow authenticated requests to add items that do not exist in store to user cart" in {

      val token = authenticateUserAndGetToken("user5", "userSekret", tokenSecret)
      val authHeader = Authorization(OAuth2BearerToken(token))

      val request: HttpRequest = Put("/user/cart", UpdateUserCartRequest("please dont exist item - im haxxxer", 999999))
        .addHeader(authHeader)

      request ~> route ~> check {
        status shouldBe BadRequest
        handled shouldBe true
        entityAs[String] shouldBe "Item does not exist in store"
      }
    }

  }

}
