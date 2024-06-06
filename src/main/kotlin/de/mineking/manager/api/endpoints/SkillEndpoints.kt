package de.mineking.manager.api.endpoints

import de.mineking.databaseutils.exception.ConflictException
import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.data.ResourceTable
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass

fun SkillEndpoints() {
	get {
		with(it) {
			checkAuthorization()
			json(main.skills.getAll(ResourceTable.DEFAULT_ORDER))
		}
	}

	post {
		with(it) {
			checkAuthorization(admin = true)

			data class Request(val name: String)

			val request = bodyAsClass<Request>()

			try {
				json(main.skills.create(request.name))
			} catch (_: ConflictException) {
				throw ErrorResponse(ErrorResponseType.SKILL_ALREADY_EXISTS)
			}
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

				data class Request(val name: String)

				val request = bodyAsClass<Request>()

				val skill = main.skills.getById(pathParam("id")) ?: throw ErrorResponse(ErrorResponseType.SKILL_NOT_FOUND)

				try {
					json(skill.copy(name = request.name).update())
				} catch (_: ConflictException) {
					throw ErrorResponse(ErrorResponseType.SKILL_ALREADY_EXISTS)
				}
			}
		}
	}
}