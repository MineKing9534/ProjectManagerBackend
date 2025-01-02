package de.mineking.manager.api.endpoints

import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.data.ResourceTable
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass

fun SkillEndpoints() {
	path("groups", ::SkillGroupEndpoints)

	get {
		with(it) {
			checkAuthorization()
			json(main.skills.getAll(ResourceTable.DEFAULT_ORDER))
		}
	}

	post {
		with(it) {
			checkAuthorization(admin = true)

			data class Request(val name: String, val group: String?)

			val request = bodyAsClass<Request>()

			val result = main.skills.create(
				name = request.name,
				group = if (request.group.isNullOrEmpty()) "" else {
					val group = main.skillGroups.getById(request.group) ?: throw ErrorResponse(ErrorResponseType.SKILL_GROUP_NOT_FOUND)
					group.id.asString()
				}
			)

			if (result.uniqueViolation) throw ErrorResponse(ErrorResponseType.SKILL_ALREADY_EXISTS)
			json(result.getOrThrow())
		}
	}

	path("{id}") {
		get {
			with(it) {
				checkAuthorization()
				json(main.skills.getById(pathParam("id")) ?: throw ErrorResponse(ErrorResponseType.SKILL_NOT_FOUND))
			}
		}

		delete {
			with(it) {
				checkAuthorization(admin = true)
				if (!main.skills.delete(pathParam("id"))) throw ErrorResponse(ErrorResponseType.SKILL_NOT_FOUND)
			}
		}

		patch {
			with(it) {
				checkAuthorization(admin = true)

				data class Request(val name: String?, val group: String?)

				val request = bodyAsClass<Request>()

				val skill = main.skills.getById(pathParam("id")) ?: throw ErrorResponse(ErrorResponseType.SKILL_NOT_FOUND)

				val result = skill.copy(
					name = request.name ?: skill.name,
					group = if (request.group != null) {
						if (request.group.isBlank()) ""
						else {
							val group = main.skillGroups.getById(request.group) ?: throw ErrorResponse(ErrorResponseType.SKILL_GROUP_NOT_FOUND)
							group.id.asString()
						}
					} else skill.group
				).update()

				if (result.uniqueViolation) throw ErrorResponse(ErrorResponseType.SKILL_ALREADY_EXISTS)
				json(result.getOrThrow())
			}
		}
	}
}