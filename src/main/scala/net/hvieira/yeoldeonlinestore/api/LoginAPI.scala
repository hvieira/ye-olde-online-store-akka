package net.hvieira.yeoldeonlinestore.api

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.model.{ContentTypeRange, HttpEntity, MediaTypes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import net.hvieira.yeoldeonlinestore.auth.Authentication

private object LoginAPI {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName

    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }

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
}

class LoginAPI(private val tokenSecret: String)(implicit system: ActorSystem)
  extends Directives
    with APIJsonSupport {

  private val log = Logging(system, this)

  import LoginAPI._
  val route: Route = path("login") {
    post {
      decodeRequest {
        entity(as[LoginData]) { loginData =>
          performLogin(loginData)
        }
      }
    }
  }

  private def performLogin(loginData: LoginData): Route = {

    val token = Authentication.generateTokenForUser(loginData.username, loginData.encryptedPassword, tokenSecret)

    log.debug("Returning token {}", token)
    complete(LoginResult(token))
  }

}
