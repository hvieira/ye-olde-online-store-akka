package net.hvieira.yeoldeonlinestore.api

import org.scalatest.{Matchers, WordSpec}

import spray.json._

class APIJsonSupportSpec extends WordSpec with Matchers with APIJsonSupport {

  "A purchase result" when {

    "successful" should {
      "serialize and deserialize properly" in {
        val originalValue = SuccessfulPurchase(
          Cart(
            Map(
              "anItemId" -> ItemAndQuantity(Item("item243", 67.01), 1),
              "anotherItemIdz" -> ItemAndQuantity(Item("item7", 7.30), 3)
            )
          ))
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
