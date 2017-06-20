package net.hvieira.yeoldeonlinestore.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json._

trait APIJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val itemFormat: RootJsonFormat[Item] = jsonFormat2(Item)
  implicit val cartFormat: RootJsonFormat[Cart] = jsonFormat1(Cart)
  implicit val updateUserCartRequestFormat: RootJsonFormat[UpdateUserCartRequest] = jsonFormat2(UpdateUserCartRequest)
  implicit val authTokenFormat: RootJsonFormat[LoginResult] = jsonFormat1(LoginResult)
  implicit val storeFrontFormat: RootJsonFormat[StoreFront] = jsonFormat1(StoreFront)
}