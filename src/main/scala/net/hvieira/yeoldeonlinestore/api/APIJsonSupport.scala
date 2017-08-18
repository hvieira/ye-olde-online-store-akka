package net.hvieira.yeoldeonlinestore.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, _}

trait APIJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val itemFormat: RootJsonFormat[Item] = jsonFormat2(Item)
  implicit val itemAndQuantityFormat: RootJsonFormat[ItemAndQuantity] = jsonFormat2(ItemAndQuantity)
  implicit val cartFormat: RootJsonFormat[Cart] = jsonFormat1(Cart)
  implicit val updateUserCartRequestFormat: RootJsonFormat[UpdateUserCartRequest] = jsonFormat2(UpdateUserCartRequest)
  implicit val authTokenFormat: RootJsonFormat[LoginResult] = jsonFormat1(LoginResult)
  implicit val storeFrontFormat: RootJsonFormat[StoreFront] = jsonFormat1(StoreFront)

  implicit object SuccessfulPurchaseFormat extends RootJsonFormat[SuccessfulPurchase] {
    def write(p: SuccessfulPurchase) = JsObject(Map("success" -> p.success.toJson, "items" -> p.items.toJson))

    def read(value: JsValue) = value.asJsObject.getFields("items") match {
      case Seq(items) => SuccessfulPurchase(items.convertTo[List[Item]])
    }
  }

  implicit object FailedPurchaseFormat extends RootJsonFormat[FailedPurchase] {
    def write(p: FailedPurchase) = JsObject(Map("success" -> p.success.toJson, "failureReason" -> p.failureReason.toJson))

    def read(value: JsValue) = value.asJsObject.getFields("failureReason") match {
      case Seq(JsString(reason)) => FailedPurchase(reason)
    }
  }

  implicit object PurchaseResultFormat extends RootJsonFormat[PurchaseResult] {
    def write(pr: PurchaseResult) = pr match {
      case s: SuccessfulPurchase => s.toJson
      case f: FailedPurchase => f.toJson
    }

    def read(value: JsValue) =
      value.asJsObject.fields.get("success") match {
        case Some(JsBoolean(true)) => value.convertTo[SuccessfulPurchase]
        case Some(JsBoolean(false)) => value.convertTo[FailedPurchase]
      }
  }

}