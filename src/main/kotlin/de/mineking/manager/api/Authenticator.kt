package de.mineking.manager.api

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.InvalidClaimException
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.DecodedJWT
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.ParentType
import de.mineking.manager.data.User
import de.mineking.manager.data.type.Resource
import de.mineking.manager.main.Main
import io.javalin.http.Context
import io.javalin.http.InternalServerErrorResponse
import java.time.Duration
import java.time.Instant
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class Authenticator(private val main: Main) {
	@OptIn(ExperimentalEncodingApi::class)
	private val algorithm = Algorithm.HMAC512(Base64.decode(main.credentials["AUTH_SECRET"] ?: throw NullPointerException("AUTH_SECRET required")))

	private val userVerifier = JWT.require(algorithm)
		.withIssuer("USER")
		.build()

	val inviteVerifier: JWTVerifier = JWT.require(algorithm)
		.withIssuer("INVITE")
		.build()

	val emailVerifier: JWTVerifier = JWT.require(algorithm)
		.withIssuer("VERIFICATION")
		.build()

	fun generateUserToken(user: User): String = JWT.create()
		.withIssuer("USER")
		.withSubject(user.id?.asString())
		.withClaim("ver", user.hashCode())
		.withExpiresAt(Instant.now().plus(Duration.ofHours(4)))
		.sign(algorithm)

	fun generateInviteToken(resource: Resource): String = JWT.create()
		.withIssuer("INVITE")
		.withSubject(resource.id!!.asString())
		.withClaim("type", resource.resourceType.name)
		.sign(algorithm)

	fun generateVerificationToken(firstName: String, lastName: String, email: String, parent: String, parentType: ParentType): String = JWT.create()
		.withIssuer("VERIFICATION")
		.withSubject(email)
		.withClaim("fn", firstName)
		.withClaim("ln", lastName)
		.withClaim("pi", parent)
		.withClaim("pt", parentType.name)
		.withExpiresAt(Instant.now().plus(Duration.ofMinutes(15)))
		.sign(algorithm)

	fun checkAuthorization(ctx: Context, type: TokenType = TokenType.USER) = checkAuthorization(ctx.header("Authorization") ?: ctx.formParam("Authorization"), type = type)

	fun checkAuthorization(token: String?, type: TokenType = TokenType.USER): AuthorizationInfo {
		return type.create(
			main,
			token.verify(
				when (type) {
					TokenType.USER -> userVerifier
					TokenType.INVITE -> inviteVerifier
					TokenType.EMAIL -> emailVerifier
				}
			)
		)
	}

	private fun String?.verify(verifier: JWTVerifier): DecodedJWT {
		if (this.isNullOrBlank()) throw ErrorResponse(ErrorResponseType.MISSING_TOKEN)

		try {
			return verifier.verify(this)
		} catch (e: Exception) {
			when (e) {
				is TokenExpiredException -> throw ErrorResponse(ErrorResponseType.TOKEN_EXPIRED)
				is SignatureVerificationException,
				is InvalidClaimException,
				is JWTDecodeException -> throw ErrorResponse(ErrorResponseType.INVALID_TOKEN)

				else -> throw InternalServerErrorResponse(e.message ?: "")
			}
		}
	}
}

enum class TokenType {
	USER {
		override fun create(main: Main, jwt: DecodedJWT): AuthorizationInfo {
			val id = jwt.subject
			val user = main.users.getById(id) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)

			if (jwt.getClaim("ver").asInt() != user.hashCode()) throw ErrorResponse(ErrorResponseType.TOKEN_EXPIRED)

			return AuthorizationInfo(main, jwt, user)
		}
	},
	INVITE,
	EMAIL;

	open fun create(main: Main, jwt: DecodedJWT): AuthorizationInfo = AuthorizationInfo(main, jwt)
}