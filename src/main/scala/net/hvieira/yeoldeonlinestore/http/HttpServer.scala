package net.hvieira.yeoldeonlinestore.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RejectionHandler, Route, UnsupportedRequestContentTypeRejection}
import akka.stream.ActorMaterializer

import scala.util.{Failure, Success}

class HttpServer(
                 implicit val system: ActorSystem,
                 implicit val materializer: ActorMaterializer) {

  def start(address: String, port: Int, route: Route) = {

    implicit val executor = system.dispatcher

    val bindingFuture = Http().bindAndHandle(route, address, port)

    bindingFuture.onComplete {
      case Success(Http.ServerBinding(_)) => println(s"Server online at http://localhost:$port")
      case Failure(t) => {
        sys.error(s"Fatal Error! Exiting")
        t.printStackTrace()
        sys.exit()
      }
    }
  }
}
