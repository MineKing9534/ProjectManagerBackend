package de.mineking.manager.api

import com.auth0.jwt.interfaces.DecodedJWT
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.User
import de.mineking.manager.main.Main

class AuthorizationInfo(val main: Main, val jwt: DecodedJWT, private val user: User? = null) {
	fun isUser() = user != null
	fun user() = user ?: throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)
}
