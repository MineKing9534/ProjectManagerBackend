package de.mineking.manager.api.endpoints

import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.main.verifyPassword
import io.javalin.http.Context
import io.javalin.http.bodyAsClass

fun LoginEndpoint(ctx: Context) = with(ctx) {
	data class Request(val email: String, val password: String)
	data class Response(val token: String)

	val request = bodyAsClass<Request>()

	val user = main.users.getByEmail(request.email) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)
	if (!verifyPassword(user.password, request.password)) throw ErrorResponse(ErrorResponseType.WRONG_PASSWORD)

	json(Response(main.authenticator.generateUserToken(user)))
}