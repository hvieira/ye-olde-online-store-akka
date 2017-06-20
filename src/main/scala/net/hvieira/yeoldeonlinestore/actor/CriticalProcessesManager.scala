package net.hvieira.yeoldeonlinestore.actor

import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager._
import net.hvieira.yeoldeonlinestore.actor.store.StoreManager
import net.hvieira.yeoldeonlinestore.actor.user.UserManager

object CriticalProcessesManager {
  private val STORE_MANAGER = "store-manager"
  private val USER_MANAGER = "user-manager"

  def props() = Props(new CriticalProcessesManager)
}

class CriticalProcessesManager() extends Actor {

  override def preStart(): Unit = {
    context.actorOf(StoreManager.props(3), STORE_MANAGER)
    context.actorOf(UserManager.props(), USER_MANAGER)
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ => SupervisorStrategy.restart
  }

  override def receive: Receive = {
    case IntroduceUserManagerReq =>
      val possibleRef = context.child(USER_MANAGER)
      possibleRef match {
        case Some(ref) => sender ! IntroductionResponse(ref)
        case None => throw new IllegalStateException("User manager actor ref does not exist")
      }

    case IntroduceStoreManagerReq =>
      val possibleRef = context.child(STORE_MANAGER)
      possibleRef match {
        case Some(ref) => sender ! IntroductionResponse(ref)
        case None => throw new IllegalStateException("Store manager actor ref does not exist")
      }
  }
}

case object IntroduceUserManagerReq
case object IntroduceStoreManagerReq

final case class IntroductionResponse(ref: ActorRef)
