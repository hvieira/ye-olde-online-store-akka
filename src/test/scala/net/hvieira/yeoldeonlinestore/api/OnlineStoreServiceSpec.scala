package net.hvieira.yeoldeonlinestore.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager
import net.hvieira.yeoldeonlinestore.test.ServiceIntegrationTest
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import spray.json._

class OnlineStoreServiceSpec extends ServiceIntegrationTest {

  // TODO probably want to change this to use the akka testkit probe actors and such or with DI
  private val testActorSystem = ActorSystem("test-system")
  private val rootProcessManager = testActorSystem.actorOf(CriticalProcessesManager.props("testSecret"))

  val route = new OnlineStoreService(rootProcessManager).route

  "The login service" should {

    "login users" in {

      val request: HttpRequest = Post("/login",
        HttpEntity(
          ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),
          "username=Johnny&password=Bravo"))

      request ~> route ~> check {
        status shouldBe OK
        handled shouldBe true

        val responseBodyAsJson = entityAs[String].parseJson.asJsObject
        println(responseBodyAsJson)
        responseBodyAsJson.fields should contain key "access_token"
        responseBodyAsJson.fields("access_token").toString() should fullyMatch regex "(.+)\\.(.+)\\.(.+)"
      }
    }

  }

  "return bad request if content type is unexpected" in {

    val request: HttpRequest = Post("/login",
      HttpEntity(
        ContentType(MediaTypes.`application/javascript`, HttpCharsets.`UTF-8`),
        "some l33t hax code"))

    request ~> route ~> check {
      status shouldBe BadRequest
      handled shouldBe true
    }
  }

  "return bad request if expected data is missing or empty" in {

    val request: HttpRequest = Post("/login",
      HttpEntity(
        ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),
        "username=&password="))

    request ~> route ~> check {
      status shouldBe BadRequest
      handled shouldBe true
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

}