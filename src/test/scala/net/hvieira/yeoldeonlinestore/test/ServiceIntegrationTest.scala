package net.hvieira.yeoldeonlinestore.test

import akka.http.scaladsl.testkit.ScalatestRouteTest
import net.hvieira.yeoldeonlinestore.api.APIJsonSupport
import net.hvieira.yeoldeonlinestore.auth.Authentication
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

abstract class ServiceIntegrationTest
  extends WordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalatestRouteTest
    with APIJsonSupport {

  protected def authenticateUserAndGetToken(username: String, password: String, tokenSecret: String): String = {
    Authentication.tokenGenerator(tokenSecret)(username)
  }

}
