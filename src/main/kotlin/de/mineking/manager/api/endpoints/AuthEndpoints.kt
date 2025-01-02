package de.mineking.manager.api.endpoints

import de.mineking.manager.api.TokenType
import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.data.ResourceType
import de.mineking.manager.main.hashPassword
import de.mineking.manager.main.verifyPassword
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.bodyAsClass

fun validatePassword(password: String) {
	if (password.isBlank()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
	if (password.length < 8) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
}

fun AuthEndpoints() {
	post("login") {
		with(it) {
			data class Request(val email: String, val password: String)
			data class Response(val token: String)

			val request = bodyAsClass<Request>()

			val user = main.users.getByEmail(request.email) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)
			if (!verifyPassword(user.password, request.password)) throw ErrorResponse(ErrorResponseType.WRONG_PASSWORD)

			json(Response(main.authenticator.generateUserToken(user)))
		}
	}

	post("verify") {
		with(it) {
			val auth = checkAuthorization(type = TokenType.EMAIL)

			data class Request(val password: String)

			val request = bodyAsClass<Request>()

			val email = auth.jwt.subject
			val firstName = auth.jwt.getClaim("fn").asString()
			val lastName = auth.jwt.getClaim("ln").asString()
			val password = request.password

			validatePassword(password)

			val id = auth.jwt.getClaim("pi").asString()
			val type = ResourceType.valueOf(auth.jwt.getClaim("pt").asString())

			val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

			val result = main.users.create(firstName, lastName, email, hashPassword(password))
			if (result.uniqueViolation) throw ErrorResponse(ErrorResponseType.USER_ALREADY_EXISTS)

			main.participants.join(result.getOrThrow().id, resource.id, type)
		}
	}

	post("reset-password") {
		with(it) {
			data class Request(val email: String)

			val request = bodyAsClass<Request>()
			val user = main.users.getByEmail(request.email) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)

			main.email.sendPasswordResetEmail(user)
		}
	}

	post("change-password") {
		with(it) {
			val auth = checkAuthorization(type = TokenType.PASSWORD)

			data class Request(val password: String)

			val request = bodyAsClass<Request>()

			val user = main.users.getById(auth.jwt.subject) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)

			user.copy(password = hashPassword(request.password)).update()
		}
	}
}