package de.mineking.manager.api.endpoints

import de.mineking.databaseutils.exception.ConflictException
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

			try {
				json(main.skillGroups.create(request.name))
			} catch (_: ConflictException) {
				throw ErrorResponse(ErrorResponseType.SKILL_GROUP_ALREADY_EXISTS)
			}
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

				try {
					json(group.copy(name = request.name).update())
				} catch (_: ConflictException) {
					throw ErrorResponse(ErrorResponseType.SKILL_GROUP_ALREADY_EXISTS)
				}
			}
		}
	}
}