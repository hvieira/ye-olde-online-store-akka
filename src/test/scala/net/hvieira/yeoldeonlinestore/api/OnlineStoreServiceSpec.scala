package net.hvieira.yeoldeonlinestore.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager
import net.hvieira.yeoldeonlinestore.test.ServiceIntegrationTest
import spray.json._

class OnlineStoreServiceSpec extends ServiceIntegrationTest {

  // TODO probably want to change this to use the akka testkit probe actors and such or with DI
  private val testActorSystem = ActorSystem("test-system")
  private val rootProcessManager = testActorSystem.actorOf(CriticalProcessesManager.props("testSecret"))

  val route = new OnlineStoreService(rootProcessManager).route

  "The login API" should {

    "login users" in {

      val request: HttpRequest = Post("/login",
        HttpEntity(
          ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),
          "username=Johnny&password=Bravo"))

      request ~> route ~> check {
        status shouldBe OK
        handled shouldBe true

        val responseBodyAsJson = entityAs[String].parseJson.asJsObject
        responseBodyAsJson.fields should contain key "access_token"
        responseBodyAsJson.fields("access_token").toString() should fullyMatch regex "(.+)\\.(.+)\\.(.+)"
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

    "allow authenticated requests to retrieve user cart" in {

      val token = authenticateUser("user1", "userSekret")
      val authHeader = Authorization(OAuth2BearerToken(token))

      val request: HttpRequest = Get("/user/cart").addHeader(authHeader)

      request ~> route ~> check {
        status shouldBe OK
        handled shouldBe true

        val cart = entityAs[Cart]
        cart.itemsToQuantityMap() shouldBe empty
      }
    }

    "allow authenticated requests to update user cart" in {

      val token = authenticateUser("user1", "userSekret")
      val authHeader = Authorization(OAuth2BearerToken(token))

      {
        val request: HttpRequest = Get("/user/cart").addHeader(authHeader)

        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true

          val cart = entityAs[Cart]
          cart.itemsToQuantityMap() shouldBe empty
        }
      }

      {
        val request: HttpRequest = Put("/user/cart")
          .addHeader(authHeader)
          // TODO there should be a way to marshal the entity object and provide the content type automatically
          .withEntity(ContentTypes.`application/json`, UpdateUserCartRequest("anItem", 2).toJson.compactPrint)

        request ~> route ~> check {
          status shouldBe OK
          handled shouldBe true

          val cart = entityAs[Cart]
          cart.itemsToQuantityMap() should contain (
            // TODO introduce the concept of product portfolio with items and respective prices
            "anItem" -> (2, 0.0)
          )
        }
      }

    }

  }

  "The store API" should {

    "return not found on resources that do not exist" in {

      val request: HttpRequest = Get("/nonExistingEndpoint")

      request ~> route ~> check {
        status shouldBe NotFound
        handled shouldBe true
      }
    }
  }

  override def afterAll(): Unit = {
    Http().shutdownAllConnectionPools()
  }
  private def authenticateUser(username: String, password: String): String = {
    val request: HttpRequest = Post("/login",
      HttpEntity(
        ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),
        s"""username=${username}&password=${password}"""))

    request ~> route ~> check {
      status shouldBe OK
      handled shouldBe true

      val responseBodyAsJson = entityAs[String].parseJson.asJsObject
      return responseBodyAsJson.fields("access_token").convertTo[String]
    }
  }

}