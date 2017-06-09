package net.hvieira.yeoldeonlinestore.test

import akka.http.scaladsl.testkit.ScalatestRouteTest
import net.hvieira.yeoldeonlinestore.api.APIJsonSupport
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

abstract class ServiceIntegrationTest
  extends WordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalatestRouteTest
    with APIJsonSupport
