package net.hvieira.yeoldeonlinestore.test

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

abstract class ServiceIntegrationTest
  extends WordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalatestRouteTest
