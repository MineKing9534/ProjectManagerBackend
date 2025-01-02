package de.mineking.manager.api.endpoints

import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.data.ResourceTable
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass

fun SkillGroupEndpoints() {
	get {
		with(it) {
			checkAuthorization()
			json(main.skillGroups.getAll(ResourceTable.DEFAULT_ORDER))
		}
	}

	post {
		with(it) {
			checkAuthorization(admin = true)

			data class Request(val name: String)

			val request = bodyAsClass<Request>()

			val result = main.skillGroups.create(request.name)

			if (result.uniqueViolation) throw ErrorResponse(ErrorResponseType.SKILL_GROUP_ALREADY_EXISTS)
			json(result.getOrThrow())

		}
	}

	path("{id}") {
		get {
			with(it) {
				checkAuthorization()
				json(main.skillGroups.getById(pathParam("id")) ?: throw ErrorResponse(ErrorResponseType.SKILL_GROUP_NOT_FOUND))
			}
		}

		delete {
			with(it) {
				checkAuthorization(admin = true)
				if (!main.skillGroups.delete(pathParam("id"))) throw ErrorResponse(ErrorResponseType.SKILL_GROUP_NOT_FOUND)
			}
		}

		patch {
			with(it) {
				checkAuthorization(admin = true)

				data class Request(val name: String)

				val request = bodyAsClass<Request>()

				val group = main.skillGroups.getById(pathParam("id")) ?: throw ErrorResponse(ErrorResponseType.SKILL_GROUP_NOT_FOUND)

				val result = group.copy(name = request.name).update()

				if (result.uniqueViolation) throw ErrorResponse(ErrorResponseType.SKILL_GROUP_ALREADY_EXISTS)
				json(result.getOrThrow())
			}
		}
	}
}