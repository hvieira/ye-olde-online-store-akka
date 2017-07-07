package net.hvieira.yeoldeonlinestore.api

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import net.hvieira.yeoldeonlinestore.actor.store.StoreManager
import net.hvieira.yeoldeonlinestore.test.ServiceIntegrationTest

class StoreAPISpec extends ServiceIntegrationTest {

  // TODO probably want to change this to use the akka testkit probe actors and such or with DI
  private val testActorSystem = ActorSystem("test-system")
  private val availableItems = List(
    Item("item1", 0.05),
    Item("123Item", 1.0),
    Item("Ring of Powaaa", 100000000.0)
  )
  private val storeManRef = testActorSystem.actorOf(StoreManager.props(3, () => availableItems))

  private val route = Route.seal(new StoreAPI(storeManRef).route)

  "The store API" should {

    "allow unauthenticated users to see items on sale" in {
      val request: HttpRequest = Get("/store")

      request ~> route ~> check {
        status shouldBe OK
        handled shouldBe true

        val storeFront = entityAs[StoreFront]
        storeFront.items should contain theSameElementsAs availableItems
      }

    }

  }

}
