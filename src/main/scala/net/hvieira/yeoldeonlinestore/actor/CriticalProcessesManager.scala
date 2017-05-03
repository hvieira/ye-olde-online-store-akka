package net.hvieira.yeoldeonlinestore.actor

import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager._

object CriticalProcessesManager {
  private val AUTHENTICATOR = "authenticator"

  val props = Props[CriticalProcessesManager]

  case object IntroduceAuthenticatorReq
  case class IntroduceAuthenticatorResp(val ref: ActorRef)
}

class CriticalProcessesManager extends Actor {

  override def preStart(): Unit = {
    context.actorOf(Authenticator.props, "authenticator")
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ => SupervisorStrategy.restart
  }

  override def receive: Receive = {
    case IntroduceAuthenticatorReq => {
      val ref = context.child(AUTHENTICATOR)
      ref match {
        case Some(ref) => sender ! IntroduceAuthenticatorResp(ref)
        case None => throw new IllegalStateException("Authenticator process ref does not exist")
      }
    }
  }
}
