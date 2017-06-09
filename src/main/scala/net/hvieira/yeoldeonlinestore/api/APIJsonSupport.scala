package net.hvieira.yeoldeonlinestore.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import net.hvieira.yeoldeonlinestore.actor.TokenPayload
import spray.json.DefaultJsonProtocol
import spray.json._

trait APIJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val tokenPayloadFormat: RootJsonFormat[TokenPayload] = jsonFormat1(TokenPayload)
  implicit val itemFormat: RootJsonFormat[Item] = jsonFormat2(Item)
  implicit val cartFormat: RootJsonFormat[Cart] = jsonFormat1(Cart)
  implicit val updateUserCartRequestFormat: RootJsonFormat[UpdateUserCartRequest] = jsonFormat2(UpdateUserCartRequest)

}

case class UpdateUserCartRequest(itemId: String, amount: Int)