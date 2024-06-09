package de.mineking.manager.api.endpoints

import de.mineking.databaseutils.exception.ConflictException
import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.api.paginateResult
import de.mineking.manager.data.MeetingTable
import de.mineking.manager.data.MeetingType
import de.mineking.manager.data.ResourceTable
import de.mineking.manager.data.ResourceType
import de.mineking.manager.main.EmailType
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass
import java.time.Instant

fun ProjectEndpoints() {
	get {
		with(it) {
			checkAuthorization(admin = true)

			paginateResult(main.projects.rowCount, main.projects::getAll, ResourceTable.DEFAULT_ORDER)
		}
	}

	post {
		with(it) {
			checkAuthorization(admin = true)

			data class Request(val name: String, val location: String, val time: Instant)

			val request = bodyAsClass<Request>()

			try {
				json(main.projects.create(
					name = request.name,
					location = request.location,
					time = request.time
				))
			} catch (_: ConflictException) {
				throw ErrorResponse(ErrorResponseType.PROJECT_ALREADY_EXISTS)
			}
		}
	}

	path("{id}") {
		before {
			with(it) {
				attribute("type", ResourceType.PROJECT)
			}
		}

		path("files", ::FileEndpoints)
		path("meetings", ::ResourceMeetingsEndpoints)
		path("users", ::MembersEndpoints)

		get {
			with(it) {
				val auth = checkAuthorization()

				val id = pathParam("id")
				val project = main.projects.getById(id) ?: throw ErrorResponse(ErrorResponseType.PROJECT_NOT_FOUND)

				if (!auth.user.admin && !project.canBeAccessed(auth.user.id.asString())) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

				json(project)
			}
		}

		delete {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				if (!main.projects.delete(id)) throw ErrorResponse(ErrorResponseType.PROJECT_NOT_FOUND)
			}
		}

		patch {
			with(it) {
				checkAuthorization(admin = true)

				data class Request(val name: String?, val parent: String?, val location: String?, val time: Instant?)

				val request = bodyAsClass<Request>()

				val id = pathParam("id")

				val project = main.projects.getById(id) ?: throw ErrorResponse(ErrorResponseType.PROJECT_NOT_FOUND)
				val meeting = main.meetings.getById(id) ?: throw ErrorResponse(ErrorResponseType.UNKNOWN)

				try {
					json(
						project.copy(
							name = request.name ?: project.name,
							parent = if (request.parent != null) {
								if (request.parent.isEmpty()) ""
								else {
									val temp = main.teams.getById(request.parent) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)
									"TEAM:${ temp.id.asString() }"
								}
							} else meeting.parent
						).update()
					)

					meeting.copy(
						name = request.name ?: project.name,
						location = request.location ?: meeting.location,
						time = request.time ?: meeting.time
					).update()
				} catch (_: ConflictException) {
					throw ErrorResponse(ErrorResponseType.PROJECT_ALREADY_EXISTS)
				}
			}
		}

		post("invites") {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				val project = main.projects.getById(id) ?: throw ErrorResponse(ErrorResponseType.PROJECT_NOT_FOUND)

				data class Response(val token: String)

				json(Response(main.authenticator.generateInviteToken(project)))
			}
		}
	}
}