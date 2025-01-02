package de.mineking.manager.api.endpoints

import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.data.InputType
import de.mineking.manager.main.JSON
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass

fun InputEndpoints() {
	get {
		with(it) {
			checkAuthorization()
			json(main.inputs.select().list())
		}
	}

	post {
		with(it) {
			checkAuthorization(admin = true)

			data class Request(val name: String, val placeholder: String, val type: InputType, val config: Any?)

			val request = bodyAsClass<Request>()

			val result = main.inputs.create(
				name = request.name,
				placeholder = request.placeholder,
				type = request.type,
				config = JSON.toJson(request.config) ?: "{}"
			)

			if (result.uniqueViolation) throw ErrorResponse(ErrorResponseType.INPUT_ALREADY_EXISTS)
			json(result.getOrThrow())
		}
	}

	path("{id}") {
		delete {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				if (!main.inputs.delete(id)) throw ErrorResponse(ErrorResponseType.INPUT_NOT_FOUND)
			}
		}

		patch {
			with(it) {
				checkAuthorization(admin = true)

				data class Request(val name: String?, val placeholder: String?, val config: Any?)

				val request = bodyAsClass<Request>()

				val id = pathParam("id")
				val input = main.inputs.getById(id) ?: throw ErrorResponse(ErrorResponseType.INPUT_NOT_FOUND)

				val result = input.copy(
					name = request.name ?: input.name,
					placeholder = request.placeholder ?: input.placeholder,
					config = if (request.config != null) JSON.toJson(request.config) else input.config
				).update()

				if (result.uniqueViolation) throw ErrorResponse(ErrorResponseType.INPUT_ALREADY_EXISTS)
				json(result.getOrThrow())
			}
		}
	}
}