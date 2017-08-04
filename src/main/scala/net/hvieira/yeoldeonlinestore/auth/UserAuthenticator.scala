package net.hvieira.yeoldeonlinestore.auth

import net.hvieira.yeoldeonlinestore.user.User

trait UserAuthenticator {

  def authenticate(username: String, password: String): Option[User]

}
