package net.hvieira.yeoldeonlinestore

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LogSource
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import net.hvieira.yeoldeonlinestore.actor._
import net.hvieira.yeoldeonlinestore.api._
import net.hvieira.yeoldeonlinestore.auth.{Authentication, PresetInMemoryUserAuthenticator}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object OnlineStoreService {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName

    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }
}

class OnlineStoreService(val rootProcessManager: ActorRef, val tokenSecret: String)
                        (implicit val system: ActorSystem, implicit val materializer: ActorMaterializer)
  extends Directives
    with APIJsonSupport {

  private val storeManagerRef = requestStoreManagerRef()
  private val userManagerRef = requestUserManagerRef()
  private val requestTokenAuthenticator = Authentication.requestTokenAuthenticator(tokenSecret)

  val loginAPI = new LoginAPI(Authentication.tokenGenerator(tokenSecret), new PresetInMemoryUserAuthenticator())
  val storeAPI = new StoreAPI(storeManagerRef)
  val userAPI = new UserAPI(userManagerRef, storeManagerRef, requestTokenAuthenticator)
  val purchaseAPI = new PurchaseAPI(userManagerRef, requestTokenAuthenticator)

  val route: Route = Route.seal(
    loginAPI.route
      ~
    storeAPI.route
      ~
    userAPI.route
      ~
    purchaseAPI.route
  )

  // TODO consider getting the reference in a better way
  private def requestUserManagerRef() = {
    implicit val timeout = Timeout(1 second)
    Await.result(rootProcessManager ? IntroduceUserManagerReq, 1 second) match {
      case IntroductionResponse(ref) => ref
      case _ => throw new IllegalStateException("Requires the user manager to exist at this point")
    }
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
