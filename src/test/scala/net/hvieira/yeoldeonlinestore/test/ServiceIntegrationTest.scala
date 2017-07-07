package net.hvieira.yeoldeonlinestore.test

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import net.hvieira.yeoldeonlinestore.api.{APIJsonSupport, LoginAPI, LoginResult}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import StatusCodes._

abstract class ServiceIntegrationTest
  extends WordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalatestRouteTest
    with APIJsonSupport {

  // TODO this can be replaced by simply executing the function which generates the access token
  protected def authenticateUserAndGetToken(username: String, password: String, tokenSecret: String): String = {

    val route = Route.seal(new LoginAPI(tokenSecret).route)

    val request: HttpRequest = Post("/login",
      HttpEntity(
        ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),
        s"""username=${username}&password=${password}"""))

    request ~> route ~> check {
      status shouldBe OK
      handled shouldBe true

      entityAs[LoginResult].authToken
    }
  }

}
