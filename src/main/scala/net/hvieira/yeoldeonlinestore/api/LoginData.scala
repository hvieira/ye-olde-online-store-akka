package net.hvieira.yeoldeonlinestore.api

final case class LoginData(val username: String, val encryptedPassword: String) {

}
