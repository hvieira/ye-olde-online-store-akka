package net.hvieira.yeoldeonlinestore

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import net.hvieira.yeoldeonlinestore.actor._
import net.hvieira.yeoldeonlinestore.actor.user.{GetUserCart, UpdateCart, UserCart}
import net.hvieira.yeoldeonlinestore.api._
import net.hvieira.yeoldeonlinestore.auth.{Authentication, TokenPayload}

import scala.concurrent.Await
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

  private def tokenAuthenticator(credentials: Credentials): Option[TokenPayload] = credentials match {
    case Credentials.Provided(token) => Authentication.authInfoFromToken(token, tokenSecret)
    case _ => None
  }

  val loginAPI = new LoginAPI(tokenSecret)
  val storeAPI = new StoreAPI(requestStoreManagerRef())

  val route = Route.seal(
    loginAPI.route
      ~
    storeAPI.route
      ~
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
    complete(LoginResult(token))
  }

  // TODO cluster methods like this into "services" that simply return the value for the API to complete/reject
  private def retrieveUserCart(user: String): Route = {

    implicit val timeout = Timeout(1 second)
    import system.dispatcher

    val cartFuture = requestUserManagerRef.flatMap {
      case IntroductionResponse(actorRef) => actorRef ? GetUserCart(user)
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
      case IntroductionResponse(actorRef) => actorRef ? UpdateCart(itemFromId(req), req.amount, user)
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

  private def requestUserManagerRef = {
    implicit val timeout = Timeout(1 second)
    rootProcessManager ? IntroduceUserManagerReq
  }

  // TODO consider getting the reference in a better way
  private def requestStoreManagerRef() = {
    implicit val timeout = Timeout(1 second)
    Await.result(rootProcessManager ? IntroduceStoreManagerReq, 1 second) match {
      case IntroductionResponse(ref) => ref
      case _ => throw new IllegalStateException("Requires the store manager to exist at this point")
    }
  }

}
