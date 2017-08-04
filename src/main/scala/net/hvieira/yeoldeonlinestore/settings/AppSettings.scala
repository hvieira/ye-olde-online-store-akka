package net.hvieira.yeoldeonlinestore.settings

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.typesafe.config.Config

object AppSettings extends ExtensionId[AppSettings] with ExtensionIdProvider {

  override def lookup = AppSettings

  override def createExtension(system: ExtendedActorSystem) =
    new AppSettings(system.settings.config)

  /**
    * Java API: retrieve the Settings extension for the given system.
    */
  override def get(system: ActorSystem): AppSettings = super.get(system)

}

class AppSettings(config: Config) extends Extension {

  val authSettings = AuthSettings(config.getString("online-store-settings.auth.token.secret"))

}

case class AuthSettings private(tokenSecret: String)
