package net.hvieira.yeoldeonlinestore.actor.store

import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props, SupervisorStrategy}
import net.hvieira.yeoldeonlinestore.actor.OperationResult.OperationResult
import net.hvieira.yeoldeonlinestore.actor._
import net.hvieira.yeoldeonlinestore.api.Item

object StoreManager {

  def props(numOfSlaves: Int, itemsProvider: () => Iterable[Item]) = Props(new StoreManager(numOfSlaves, itemsProvider))

}

class StoreManager(private val numOfSlaves: Int,
                   private val itemProvider: () => Iterable[Item])
  extends Actor
    with ActorLogging {

  override def preStart(): Unit = {
    (0 to numOfSlaves).foreach(i => context.actorOf(Props(new Worker(itemProvider)), slaveName(i)))
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _ => SupervisorStrategy.restart
  }

  def distributeLoad(slaveIndex: Int): Receive = {
    case req@StoreFrontRequest =>
      log.debug("Looking for slave for index {}", slaveIndex)
      context.child(slaveName(slaveIndex)) match {
        case Some(slaveRef) => slaveRef forward req
        case None => StoreFrontResponse(OperationResult.NOT_OK)
      }
      context.become(distributeLoad((slaveIndex + 1) % numOfSlaves), discardOld = true)
  }

  override def receive: Receive = distributeLoad(0)

  private def slaveName(i: Int) = s"slave$i"
}

private class Worker(private val itemProvider: () => Iterable[Item]) extends Actor {
  override def receive: Receive = {
    case StoreFrontRequest =>
      // This is where we would get stuff from a cache or a DB
      sender ! StoreFrontResponse(OperationResult.OK, itemProvider.apply().toList)
  }
}


case object StoreFrontRequest

final case class StoreFrontResponse(result: OperationResult, items: List[Item] = List())
