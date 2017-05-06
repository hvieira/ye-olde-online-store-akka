package net.hvieira.yeoldeonlinestore.actor

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit, TestKitBase}
import net.hvieira.yeoldeonlinestore.actor.Authenticator.{AuthenticateUser, UserAuthenticatedResp}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec, WordSpecLike}

import scala.util.Success
import akka.pattern.ask
import akka.util.Timeout
import pdi.jwt.{Jwt, JwtAlgorithm}

import scala.collection.immutable.Map
import scala.concurrent.duration._

class AuthenticatorTest
  extends TestKit(ActorSystem("testSystem"))
    with DefaultTimeout
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  "The authenticator" should {
    "create JWT tokens on authentication" in {

      val actorRef = TestActorRef(new Authenticator)

      val future = actorRef ? AuthenticateUser("someUser", "somePass")
      val Success(result: UserAuthenticatedResp) = future.value.get

      // TODO the secret is hardcoded here for now but needs to disappear once the actor receives the configuration
      val decodedToken = Jwt.decodeRawAll(result.authToken, "topSekret", Seq(JwtAlgorithm.HS256))

      // TODO this should be improved
      decodedToken should matchPattern {
        case Success((_, """{"user":"someUser"}""", _)) =>
      }

    }
  }

}
