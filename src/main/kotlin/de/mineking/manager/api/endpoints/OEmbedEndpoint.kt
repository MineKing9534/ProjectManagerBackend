package de.mineking.manager.api.endpoints

import de.mineking.manager.api.TokenType
import de.mineking.manager.api.main
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotImplementedResponse

fun OEmbedEndpoint(ctx: Context) = with(ctx) {
	data class Response(val version: String = "1.0", val type: String = "link", val provider_name: String, val author_name: String, val title: String)

	val format = queryParam("format")
	var url = queryParam("url")

	if (url == null || format == null) throw BadRequestResponse()
	if (format != "json") throw NotImplementedResponse()

	url = url.replace(main.config.url, "")

	json(
		if (url.matches("/invite\\?token=.*".toRegex())) {
			val token = url.substring("/join?token=".length)
			val auth = main.authenticator.checkAuthorization(token, type = TokenType.INVITE)

			val id = auth.jwt.subject!!
			val type = auth.jwt.getClaim("type")!!.asString()

			TODO("read object, send response")
		} else Response(provider_name = main.config.info.provider, author_name = main.config.info.author, title = main.config.info.title)
	)
}