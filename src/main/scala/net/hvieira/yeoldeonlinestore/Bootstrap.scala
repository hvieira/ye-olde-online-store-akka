package net.hvieira.yeoldeonlinestore

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager
import net.hvieira.yeoldeonlinestore.api.Item
import net.hvieira.yeoldeonlinestore.http.HttpServer

object Bootstrap extends App {

  implicit val actorSystem = ActorSystem("ye-olde-online-store")
  implicit val actorMaterializer = ActorMaterializer()

  private val tokenSecret = actorSystem.settings.config.getString("auth.token.secret")

  private val itemProvider = () => List(
    Item("health potion", 10),
    Item("mana potion", 15),
    Item("sanity potion", 10000),
    Item("stamina potion", 5),
    Item("pint of dehydrated beer", 2)
  )

  private val rootProcessesManager = actorSystem.actorOf(CriticalProcessesManager.props(itemProvider))

  new HttpServer()
    .start(
      "localhost",
      actorSystem.settings.config.getInt("server.port"),
      new OnlineStoreService(rootProcessesManager, tokenSecret).route)
}
