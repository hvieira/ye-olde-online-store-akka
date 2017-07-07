package net.hvieira.yeoldeonlinestore.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import net.hvieira.yeoldeonlinestore.OnlineStoreService
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager
import net.hvieira.yeoldeonlinestore.test.ServiceIntegrationTest

class OnlineStoreServiceSpec extends ServiceIntegrationTest {

  // TODO probably want to change this to use the akka testkit probe actors and such or with DI
  private val testActorSystem = ActorSystem("test-system")
  private val tokenSecret = "testSecret"
  private val rootProcessManager = testActorSystem.actorOf(CriticalProcessesManager.props(() => List()))

  private val route = new OnlineStoreService(rootProcessManager, tokenSecret).route

  "Generically, the API" should {

    "return not found on resources that do not exist" in {

      val request: HttpRequest = Get("/nonExistingEndpoint")

      request ~> route ~> check {
        status shouldBe NotFound
        handled shouldBe true
      }
    }
  }

}