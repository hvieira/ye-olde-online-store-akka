package net.hvieira.yeoldeonlinestore.api

import java.util.NoSuchElementException

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.Futures
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.SecurityDirectives.Authenticator
import akka.pattern.ask
import akka.util.Timeout
import net.hvieira.yeoldeonlinestore.actor.OperationResult
import net.hvieira.yeoldeonlinestore.actor.store.{ItemFromStore, ItemInStore}
import net.hvieira.yeoldeonlinestore.actor.user.{GetUserCart, UpdateCart, UserCart}
import net.hvieira.yeoldeonlinestore.auth.TokenPayload

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object UserAPI {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName

    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }
}

class UserAPI(private val userManagerRef: ActorRef,
              private val storeManagerRef: ActorRef,
              private val requestAuthenticator: Authenticator[TokenPayload])
             (implicit system: ActorSystem)
  extends Directives
    with APIJsonSupport {

  private val log = Logging(system, this)

  val route: Route = pathPrefix("user") {
    path("cart") {
      authenticateOAuth2("", requestAuthenticator) { userToken =>
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

  private def retrieveUserCart(user: String): Route = {
    implicit val timeout = Timeout(1 second)

    val cartFuture = userManagerRef ? GetUserCart(user)

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

    log.info("Retrieving item from store and then updating cart")
    val cartFuture = for {
      storeResp <- (storeManagerRef ? ItemFromStore(req.itemId)).mapTo[ItemInStore]
      updatedCart <- userManagerRef ? UpdateCart(storeResp.itemOpt.get, req.amount, user) if storeResp.itemOpt.isDefined
    } yield updatedCart

    onComplete(cartFuture) {
      case Success(UserCart(OperationResult.OK, _, cart)) =>
        log.debug("Returning updated cart {}", cart)
        complete(cart)

      case Success(UserCart(OperationResult.NOT_OK, _, _)) =>
        log.error("Failed to update cart for user {}", user)
        complete(HttpResponse(InternalServerError))

      case Failure(_: NoSuchElementException) =>
        log.error("Item does not exist in the store")
        complete(HttpResponse(BadRequest, entity = HttpEntity("Item does not exist in store")))

      case _ =>
        log.error("Unexpected result for cart retrieval flow")
        complete(HttpResponse(InternalServerError))
    }
  }

}
