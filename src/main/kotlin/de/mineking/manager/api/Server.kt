package de.mineking.manager.api

import com.google.gson.JsonSyntaxException
import de.mineking.manager.api.endpoints.*
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.Identifiable
import de.mineking.manager.data.IdentifiableTable
import de.mineking.manager.main.JSON
import de.mineking.manager.main.Main
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.config.Key
import io.javalin.http.Context
import io.javalin.http.HttpResponseException
import io.javalin.http.InternalServerErrorResponse
import io.javalin.json.JsonMapper
import io.javalin.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Type


val MAIN_KEY = Key<Main>("main")
val Context.main get() = this.appData(MAIN_KEY)

fun Context.checkAuthorization(admin: Boolean = false, type: TokenType = TokenType.USER): AuthorizationInfo {
	val auth = this.main.authenticator.checkAuthorization(this, type = type)
	if (admin && !auth.user().admin) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

	return auth
}

fun <T : Identifiable> Context.getTarget(table: IdentifiableTable<T>, error: ErrorResponseType, name: String = "id"): T {
	val auth = checkAuthorization()

	var id = pathParam(name)
	if (id == "@me") id = auth.user().id.asString()

	if (!auth.user().admin && id != auth.user().id.asString()) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

	return table.getById(id) ?: throw ErrorResponse(error)
}

fun Context.checkSuperUser() {
	val auth = this.main.credentials["SUPER_TOKEN"]
	if (auth == null || auth != header("XX_SUPER_USER_XX")) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)
}

class Server(private val main: Main) {
	companion object {
		val logger: Logger = LoggerFactory.getLogger(Server::class.java)
	}

	private val server: Javalin = Javalin.create { config ->
		config.showJavalinBanner = false

		config.http.defaultContentType = "text/json"
		config.useVirtualThreads = true

		config.bundledPlugins.enableCors { cors -> cors.addRule { it.anyHost() } }

		config.jsonMapper(object : JsonMapper {
			override fun <T : Any> fromJsonString(json: String, targetType: Type): T {
				if (json.isBlank()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
				return JSON.fromJson(json, targetType)
			}

			override fun toJsonString(obj: Any, type: Type): String {
				return JSON.toJson(obj, type)
			}
		})

		config.router.apiBuilder {
			get("oembed", ::OEmbedEndpoint)

			path("users", ::UserEndpoints)
			path("auth", ::AuthEndpoints)

			path("inputs", ::InputEndpoints)

			path("skills", ::SkillEndpoints)
			path("meetings", ::MeetingEndpoints)
			path("teams", ::TeamEndpoints)
			path("projects", ::ProjectEndpoints)
		}

		config.appData(MAIN_KEY, main)
	}

	init {
		server.after {
			with(it) {
				if (result() == null) result("{}")
			}
		}

		server.exception(JsonSyntaxException::class.java) { _, _ -> throw ErrorResponse(ErrorResponseType.INVALID_REQUEST) }
		server.exception(ValidationException::class.java) { _, _ -> throw ErrorResponse(ErrorResponseType.INVALID_REQUEST) }

		server.exception(Exception::class.java) { e, _ ->
			logger.error("Error in http handler", e)
			throw InternalServerErrorResponse()
		}

		server.exception<HttpResponseException>(HttpResponseException::class.java) { e, _ -> throw ErrorResponse(ErrorResponseType.UNKNOWN, e.status) }

		server.exception(ErrorResponse::class.java) { e, ctx ->
			try {
				ctx.status(e.status).json(e.data)
			} catch (ex: Exception) {
				logger.error("Error sending response", ex)
				ctx.status(500).json(ErrorResponse(ErrorResponseType.UNKNOWN).data)
			}
		}
	}

	fun start() = server.start(main.config.port)
	fun stop() = server.stop()
}