package net.hvieira.yeoldeonlinestore.api

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager.{IntroduceAuthenticatorReq, IntroduceAuthenticatorResp, IntroduceUserManagerReq, IntroduceUserManagerResp}
import net.hvieira.yeoldeonlinestore.actor._
import net.hvieira.yeoldeonlinestore.actor.user.{GetUserCart, UpdateCart, UserCart}
import net.hvieira.yeoldeonlinestore.auth.Authentication

import scala.concurrent.duration._
import scala.util.Success

object OnlineStoreService {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName

    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }
}

class OnlineStoreService(val rootProcessManager: ActorRef, val tokenSecret: String)
                        (implicit val system: ActorSystem, implicit val materializer: ActorMaterializer)
  extends Directives with APIJsonSupport {

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

  private def tokenAuthenticator(credentials: Credentials): Option[TokenPayload] = credentials match {
    case Credentials.Provided(token) => Authentication.authInfoFromToken(token, tokenSecret)
    case _ => None
  }

  val route = Route.seal(
    path("login") {
      post {
        decodeRequest {
          entity(as[LoginData]) {
            loginData => performLogin(loginData)
          }
        }
      }
    } ~
      pathPrefix("user") {
        path("cart") {
          authenticateOAuth2("", tokenAuthenticator) { userToken =>
            get {
              retrieveUserCart(userToken.user)
            } ~
              put {
                entity(as[UpdateUserCartRequest]) { updateUserCartRequest =>
                  updateUserCart(userToken.user, updateUserCartRequest)
                }
              }
          }
        }
      }
  )

  private def performLogin(loginData: LoginData): Route = {

    val token = Authentication.generateTokenForUser(loginData.username, loginData.encryptedPassword, tokenSecret)

    log.debug("Returning token {}", token)
    complete(HttpResponse(OK, entity = HttpEntity(ContentTypes.`application/json`, s"""{"access_token": "$token"}""")))
  }

  // TODO cluster methods like this into "services" that simply return the value for the API to complete/reject
  private def retrieveUserCart(user: String): Route = {

    implicit val timeout = Timeout(1 second)
    import system.dispatcher

    val cartFuture = requestUserManagerRef.flatMap {
      case IntroduceUserManagerResp(actorRef) => actorRef ? GetUserCart(user)
    }

    onComplete(cartFuture) {
      case Success(UserCart(OperationResult.OK, _, cart)) =>
        log.debug("Returning cart {}", cart)
        complete(cart)

      case Success(UserCart(OperationResult.NOT_OK, _, _)) =>
        log.error("Failed to retrieve cart for user {}", user)
        complete(HttpResponse(InternalServerError))

      case _ =>
        log.error("Unexpected result for cart retrieval flow")
        complete(HttpResponse(InternalServerError))
    }
  }

  def updateUserCart(user: String, req: UpdateUserCartRequest): Route = {
    implicit val timeout = Timeout(1 second)
    import system.dispatcher

    val cartFuture = requestUserManagerRef.flatMap {
      case IntroduceUserManagerResp(actorRef) => actorRef ? UpdateCart(itemFromId(req), req.amount, user)
    }

    onComplete(cartFuture) {
      case Success(UserCart(OperationResult.OK, _, cart)) =>
        log.debug("Returning updated cart {}", cart)
        complete(cart)

      case Success(UserCart(OperationResult.NOT_OK, _, _)) =>
        log.error("Failed to update cart for user {}", user)
        complete(HttpResponse(InternalServerError))

      case _ =>
        log.error("Unexpected result for cart retrieval flow")
        complete(HttpResponse(InternalServerError))
    }
  }

  private def itemFromId(req: UpdateUserCartRequest) = Item(req.itemId, 0.0)

  private def requestAutheticatorRef = {
    implicit val timeout = Timeout(1 second)
    rootProcessManager ? IntroduceAuthenticatorReq
  }

  private def requestUserManagerRef = {
    implicit val timeout = Timeout(1 second)
    rootProcessManager ? IntroduceUserManagerReq
  }

  //  private def verifyAuthorizationToken(token: String): Future[Option[TokenPayload]] = {
  //    implicit val timeout = Timeout(1 second)
  //    import system.dispatcher
  //
  //    requestAutheticatorRef.flatMap {
  //      case IntroduceAuthenticatorResp(actorRef) => actorRef ? ValidateAuthorizationToken(token)
  //    } map {
  //      case AuthorizationTokenValidated(OperationResult.OK, tokenPayload) => Some(tokenPayload)
  //      case _ => None
  //    }
  //  }
}
