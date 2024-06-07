package de.mineking.manager.api.endpoints

import de.mineking.manager.api.TokenType
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.main
import de.mineking.manager.data.ResourceType
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotImplementedResponse
import java.util.*

fun OEmbedEndpoint(ctx: Context) = with(ctx) {
	data class Response(val version: String = "1.0", val type: String = "link", val provider_name: String, val author_name: String, val title: String)

	val format = queryParam("format")
	var url = queryParam("url")

	if (url == null || format == null) throw BadRequestResponse()
	if (format != "json") throw NotImplementedResponse()

	url = url.replace(main.config.url, "")

	json(
		if (url.matches("/invite\\?token=.*".toRegex())) {
			try {
				val token = url.substring("/invite?token=".length)
				val auth = main.authenticator.checkAuthorization(token, type = TokenType.INVITE)

				val id = auth.jwt.subject!!
				val type = ResourceType.valueOf(auth.jwt.getClaim("type").asString())

				val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

				Response(provider_name = main.config.info.provider, author_name = main.config.info.author, title = "${resource.name} beitreten")
			} catch (e: ErrorResponse) {
				Response(provider_name = main.config.info.provider, author_name = main.config.info.author, title = "Ungültige Einladung")
			}
		}
		else if (url.matches("/@me/(teams|meetings)/.*".toRegex())) {
			try {
				val typeTemp = url.split("/")[2]
				val type = ResourceType.valueOf(typeTemp.uppercase().dropLast(1))

				val id = url.split("/")[3]

				val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

				Response(provider_name = main.config.info.provider, author_name = main.config.info.author, title = resource.name)
			} catch (e: ErrorResponse) {
				Response(provider_name = main.config.info.provider, author_name = main.config.info.author, title = "Ungültige Einladung")
			}
		}
		else Response(provider_name = main.config.info.provider, author_name = main.config.info.author, title = main.config.info.title)
	)
}