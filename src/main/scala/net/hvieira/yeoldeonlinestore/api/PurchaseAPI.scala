package net.hvieira.yeoldeonlinestore.api

import java.util.NoSuchElementException

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError}
import akka.http.scaladsl.server.directives.SecurityDirectives.Authenticator
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import net.hvieira.yeoldeonlinestore.actor.OperationResult
import net.hvieira.yeoldeonlinestore.actor.OperationResult.OperationResult
import net.hvieira.yeoldeonlinestore.actor.user.{GetUserCart, UserCart}
import net.hvieira.yeoldeonlinestore.auth.TokenPayload

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object PurchaseAPI {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName

    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }
}

class PurchaseAPI(private val userManagerRef: ActorRef,
                  private val requestAuthenticator: Authenticator[TokenPayload])
                 (implicit system: ActorSystem)
  extends Directives
    with APIJsonSupport {

  import system.dispatcher

  private val log = Logging(system, this)

  private def pay(user: String, totalCost: Double) = {
    // dummy implementation
    val randomizedSleepTime = Math.random() * 1000
    Thread.sleep(randomizedSleepTime.toLong)

    // would return some kind of receipt
    Future((OperationResult.OK, totalCost))
  }

  private def dispatchStock(cart: Cart) = {
    // would dispatch stock
    Future((OperationResult.OK, cart))
  }

  private def sendEmail(userId: String, purchaseResult: (OperationResult, Double)) = {
    // would find the user email and send email
    Future("anEmailUUID or something")
  }

  def checkoutCart(user: String): Route = {
    log.debug("Checking out cart for user {}", user)
    implicit val timeout = Timeout(10 second)

    val future = for {
      cartRetrieveResult <- (userManagerRef ? GetUserCart(user)).mapTo[UserCart] if cartRetrieveResult.cart.items.nonEmpty
      purchaseResult <- pay(user, cartRetrieveResult.cart.totalCost)
      stockResult <- dispatchStock(cartRetrieveResult.cart)
      emailSent <- sendEmail(user, purchaseResult)
    } yield (purchaseResult._1, cartRetrieveResult.cart)

    onComplete(future.mapTo[(OperationResult, Cart)]) {
      case Success((OperationResult.OK, cart)) =>
        log.info("Successful purchase")
        complete(SuccessfulPurchase(cart))
      case Failure(_: NoSuchElementException) =>
        log.info("User does not have items in cart for purchase")
        complete(HttpResponse(BadRequest))
      case _ =>
        log.error("Failed to purchase cart")
        complete(HttpResponse(InternalServerError))
    }
  }

  val route: Route =
    path("purchase") {
      authenticateOAuth2("", requestAuthenticator) { userToken =>
        post {
          checkoutCart(userToken.user)
        }
      }
    }

}
