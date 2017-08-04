package net.hvieira.yeoldeonlinestore.auth
import net.hvieira.yeoldeonlinestore.user.User

class PresetInMemoryUserAuthenticator extends UserAuthenticator {

  private val allowedUsers = Map(
    ("john", "mary") -> new User("b78h-6fsj-8nsa-tvn0", "john"),
    ("root","sekretz") -> new User("gd0m-gia8-gvtn-mm47", "root")
  )

  override def authenticate(username: String, password: String): Option[User] = {
    allowedUsers.get((username, password))
  }
}
