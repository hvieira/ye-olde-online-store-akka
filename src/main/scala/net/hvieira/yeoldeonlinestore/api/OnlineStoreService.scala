package net.hvieira.yeoldeonlinestore.api

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RejectionHandler, Route, UnsupportedRequestContentTypeRejection}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager.{IntroduceAuthenticatorReq, IntroduceAuthenticatorResp}
import net.hvieira.yeoldeonlinestore.actor.{AuthenticateUser, OperationResult, UserAuthenticatedResp}

import scala.concurrent.duration._
import scala.util.Success

object OnlineStoreService {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName
    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }
}

class OnlineStoreService(val rootProcessManager: ActorRef)
                        (implicit val system: ActorSystem, implicit val materializer: ActorMaterializer) {

  private val log = Logging(system, this)

  implicit val loginDataUM: Unmarshaller[HttpEntity, LoginData] = {
    Unmarshaller.urlEncodedFormDataUnmarshaller(ContentTypeRange(MediaTypes.`application/x-www-form-urlencoded`))
      .map(formData => {
        val username = formData.fields.getOrElse("username", "")
        val password = formData.fields.getOrElse("password", "")

        if (username.isEmpty || password.isEmpty) {
          throw new IllegalArgumentException("Missing critical login data")
        }

        LoginData(username, password)
      })
  }

  val loginUnsupportedContentTypeRejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case UnsupportedRequestContentTypeRejection(supported) => complete(HttpResponse(BadRequest))
      }
      .result()

  val route = Route.seal(
    path("login") {
      post {
        handleRejections(loginUnsupportedContentTypeRejectionHandler) {
          decodeRequest {
            entity(as[LoginData]) {
              loginData => performLogin(loginData)
            }
          }
        }
      }
    }
  )

  def performLogin(loginData: LoginData): Route = {

    implicit val timeout = Timeout(1 second)

    // TODO the chain of asks here is messy - find a better pattern for this
    val resp = rootProcessManager ? IntroduceAuthenticatorReq
    onComplete(resp) {
      case Success(IntroduceAuthenticatorResp(actorRef)) => {

        val authFuture = actorRef ? AuthenticateUser(loginData.username, loginData.encryptedPassword)

        onComplete(authFuture) {

          case Success(UserAuthenticatedResp(OperationResult.OK, _, token)) => {
            log.info("responding with headers")
            complete(
              HttpResponse(OK,
                entity = HttpEntity(ContentTypes.`application/json`, s"""{"access_token": "$token"}""")
              ))
          }

          case _ => complete(HttpResponse(InternalServerError))
        }

      }
      case _ => complete(HttpResponse(InternalServerError))
    }
  }
}
