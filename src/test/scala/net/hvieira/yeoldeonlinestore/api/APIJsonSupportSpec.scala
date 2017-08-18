package net.hvieira.yeoldeonlinestore.api

import org.scalatest.{Matchers, WordSpec}

import spray.json._

class APIJsonSupportSpec extends WordSpec with Matchers with APIJsonSupport {

  "A purchase result" when {

    "successful" should {
      "serialize and deserialize properly" in {
        val originalValue = SuccessfulPurchase(List(Item("1", 0.1), Item("2", 0.3), Item("b", 0.0)))
        val result = originalValue.toJson.convertTo[SuccessfulPurchase]

        result should equal(originalValue)

        result.toJson.convertTo[PurchaseResult].success shouldBe true
      }
    }

    "failure" should {
      "serialize and deserialize properly" in {
        val originalValue = FailedPurchase("error cause")
        val result = originalValue.toJson.convertTo[FailedPurchase]

        result should equal(originalValue)

        result.toJson.convertTo[PurchaseResult].success shouldBe false
      }
    }
  }

}
