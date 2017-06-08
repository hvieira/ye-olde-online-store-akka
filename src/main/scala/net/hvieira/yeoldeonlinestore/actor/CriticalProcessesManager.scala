package net.hvieira.yeoldeonlinestore.actor

import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager._
import net.hvieira.yeoldeonlinestore.actor.user.UserManager

object CriticalProcessesManager {
  private val AUTHENTICATOR = "authenticator"
  private val USER_MANAGER = "user-manager"

  def props(tokenSecret: String) = Props(new CriticalProcessesManager(tokenSecret))

  case object IntroduceAuthenticatorReq
  case class IntroduceAuthenticatorResp(ref: ActorRef)

  case object IntroduceUserManagerReq
  case class IntroduceUserManagerResp(ref: ActorRef)
}

class CriticalProcessesManager(private val tokenSecret: String) extends Actor {

  override def preStart(): Unit = {
    context.actorOf(Authenticator.props(tokenSecret), AUTHENTICATOR)
    context.actorOf(UserManager.props(), USER_MANAGER)
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ => SupervisorStrategy.restart
  }

  override def receive: Receive = {
    case IntroduceAuthenticatorReq => {
      val possibleRef = context.child(AUTHENTICATOR)
      possibleRef match {
        case Some(ref) => sender ! IntroduceAuthenticatorResp(ref)
        case None => throw new IllegalStateException("Authenticator actor ref does not exist")
      }
    }
    case IntroduceUserManagerReq => {
      val possibleRef = context.child(USER_MANAGER)
      possibleRef match {
        case Some(ref) => sender ! IntroduceUserManagerResp(ref)
        case None => throw new IllegalStateException("User manager actor ref does not exist")
      }
    }
  }
}
