package de.mineking.manager.api.endpoints

import de.mineking.databaseutils.exception.ConflictException
import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.data.ParentType
import de.mineking.manager.data.table.ResourceTable
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass

fun TeamEndpoints() {
	get {
		with(it) {
			checkAuthorization(admin = true)
			json(main.teams.getAll(ResourceTable.DEFAULT_ORDER))
		}
	}

	post {
		with(it) {
			checkAuthorization(admin = true)

			data class Request(val name: String)

			val request = bodyAsClass<Request>()

			try {
				json(main.teams.create(request.name))
			} catch (_: ConflictException) {
				throw ErrorResponse(ErrorResponseType.TEAM_ALREADY_EXISTS)
			}
		}
	}

	path("{id}") {
		before {
			with(it) {
				attribute("type", ParentType.TEAM)
			}
		}

		path("files", ::FileEndpoints)
		path("members", ::MembersEndpoints)

		get {
			with(it) {
				val auth = checkAuthorization()

				val id = pathParam("id")
				val team = main.teams.getById(id) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

				if (!auth.user.admin && auth.user.id!!.asString() !in main.participants.getParticipantUserIds(id)) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

				json(team)
			}
		}

		delete {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				if (!main.teams.delete(id)) throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)
			}
		}

		patch {
			with(it) {
				checkAuthorization(admin = true)

				data class Request(val name: String)

				val request = bodyAsClass<Request>()

				val id = pathParam("id")
				val team = main.teams.getById(id) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

				try {
					json(team.copy(name = request.name).update())
				} catch (_: ConflictException) {
					throw ErrorResponse(ErrorResponseType.TEAM_ALREADY_EXISTS)
				}
			}
		}
	}
}