package net.hvieira.yeoldeonlinestore.api

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import net.hvieira.yeoldeonlinestore.actor.{IntroductionResponse, OperationResult}
import net.hvieira.yeoldeonlinestore.actor.store.{StoreFrontRequest, StoreFrontResponse}
import akka.pattern.ask

import scala.concurrent.duration._
import scala.util.Success
import scala.language.postfixOps

object StoreAPI {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName

    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }
}

class StoreAPI(private val storeManagerRef: ActorRef)(implicit system: ActorSystem)
  extends Directives
    with APIJsonSupport {

  private val log = Logging(system, this)

  val route: Route = path("store") {
    get {
      retrieveStoreFront
    }
  }

  def retrieveStoreFront: Route = {

    implicit val timeout = Timeout(1 second)

    log.debug("retrieving store front")

    val storeFrontFuture = storeManagerRef ? StoreFrontRequest

    onComplete(storeFrontFuture) {
      case Success(StoreFrontResponse(OperationResult.OK, items)) =>
        log.debug("Returning store {}", items)
        complete(StoreFront(items))

      case Success(StoreFrontResponse(OperationResult.NOT_OK, _)) =>
        log.error("Failed to retrieve store front")
        complete(HttpResponse(InternalServerError))

      case _ =>
        log.error("Unexpected result for store front retrieval flow")
        complete(HttpResponse(InternalServerError))
    }
  }

}
