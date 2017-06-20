package net.hvieira.yeoldeonlinestore.actor

import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager._
import net.hvieira.yeoldeonlinestore.actor.user.UserManager

object CriticalProcessesManager {
  private val USER_MANAGER = "user-manager"

  def props() = Props(new CriticalProcessesManager)

  case object IntroduceUserManagerReq
  case class IntroduceUserManagerResp(ref: ActorRef)
}

class CriticalProcessesManager() extends Actor {

  override def preStart(): Unit = {
    context.actorOf(UserManager.props(), USER_MANAGER)
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ => SupervisorStrategy.restart
  }

  override def receive: Receive = {
    case IntroduceUserManagerReq => {
      val possibleRef = context.child(USER_MANAGER)
      possibleRef match {
        case Some(ref) => sender ! IntroduceUserManagerResp(ref)
        case None => throw new IllegalStateException("User manager actor ref does not exist")
      }
    }
  }
}
