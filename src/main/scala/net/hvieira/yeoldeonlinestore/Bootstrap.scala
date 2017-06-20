package net.hvieira.yeoldeonlinestore

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import net.hvieira.yeoldeonlinestore.actor.CriticalProcessesManager
import net.hvieira.yeoldeonlinestore.api.OnlineStoreService
import net.hvieira.yeoldeonlinestore.http.HttpServer

object Bootstrap extends App {

  implicit val actorSystem = ActorSystem("ye-olde-online-store")
  implicit val actorMaterializer = ActorMaterializer()

  val tokenSecret = actorSystem.settings.config.getString("auth.token.secret")

  val rootProcessesManager = actorSystem.actorOf(CriticalProcessesManager.props())

  new HttpServer()
    .start(
      "localhost",
      actorSystem.settings.config.getInt("server.port"),
      new OnlineStoreService(rootProcessesManager, tokenSecret).route)
}
