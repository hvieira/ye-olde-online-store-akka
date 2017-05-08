package net.hvieira.yeoldeonlinestore.actor

import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager._

object CriticalProcessesManager {
  private val AUTHENTICATOR = "authenticator"

  def props(tokenSecret: String) = Props(new CriticalProcessesManager(tokenSecret))

  case object IntroduceAuthenticatorReq
  case class IntroduceAuthenticatorResp(val ref: ActorRef)
}

class CriticalProcessesManager(private val tokenSecret: String) extends Actor {

  override def preStart(): Unit = {
    context.actorOf(Authenticator.props(tokenSecret), AUTHENTICATOR)
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ => SupervisorStrategy.restart
  }

  override def receive: Receive = {
    case IntroduceAuthenticatorReq => {
      val possibleRef = context.child(AUTHENTICATOR)
      possibleRef match {
        case Some(ref) => sender ! IntroduceAuthenticatorResp(ref)
        case None => throw new IllegalStateException("Authenticator process ref does not exist")
      }
    }
  }
}
