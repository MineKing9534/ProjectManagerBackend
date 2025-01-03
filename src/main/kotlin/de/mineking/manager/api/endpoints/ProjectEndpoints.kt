package de.mineking.manager.api.endpoints

import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.api.paginateResult
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

			paginateResult(main.projects.selectRowCount(), main.projects::getAll, ResourceTable.DEFAULT_ORDER)
		}
	}

	post {
		with(it) {
			val auth = checkAuthorization(admin = true)

			data class Request(val name: String, val location: String, val time: Instant)

			val request = bodyAsClass<Request>()

			val result = main.projects.create(
				name = request.name,
				location = request.location,
				time = request.time
			)

			if (result.uniqueViolation) throw ErrorResponse(ErrorResponseType.PROJECT_ALREADY_EXISTS)
			val project = result.getOrThrow()

			main.participants.join(auth.user().id, project.id, ResourceType.PROJECT)
			json(project)
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

				if (!auth.user().admin && !project.canBeAccessed(auth.user().id.asString())) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

				json(project)
			}
		}

		delete {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")

				val project = main.projects.getById(id) ?: throw ErrorResponse(ErrorResponseType.PROJECT_NOT_FOUND)
				if (!main.projects.delete(project.id.asString())) throw ErrorResponse(ErrorResponseType.PROJECT_NOT_FOUND)

				val parent = project.getParent()
				if (parent != null) {
					main.email.sendEmail(
						EmailType.PROJECT_DELETE, main.participants.getParticipantUsers(parent), arrayOf(
							parent,
							project
						)
					)
				}
			}
		}

		patch {
			with(it) {
				checkAuthorization(admin = true)

				data class Request(val name: String?, val parent: String?, val location: String?, val time: Instant?)

				val request = bodyAsClass<Request>()

				val id = pathParam("id")

				var project = main.projects.getById(id) ?: throw ErrorResponse(ErrorResponseType.PROJECT_NOT_FOUND)
				val meeting = main.meetings.getById(id) ?: throw ErrorResponse(ErrorResponseType.UNKNOWN)

				val oldParent = project.parent

				val result = project.copy(
					name = request.name ?: project.name,
					parent = if (request.parent != null) {
						if (request.parent.isEmpty()) ""
						else {
							val temp = main.teams.getById(request.parent) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)
							"TEAM:${ temp.id.asString() }"
						}
					} else meeting.parent
				).update()

				if (result.uniqueViolation)throw ErrorResponse(ErrorResponseType.PROJECT_ALREADY_EXISTS)
				project = result.getOrThrow()

				json(project)

				meeting.copy(
					name = request.name ?: project.name,
					location = request.location ?: meeting.location,
					time = request.time ?: meeting.time
				).update()

				if (project.parent != oldParent) {
					val parent = project.getParent()
					if (parent != null) {
						main.email.sendEmail(
							EmailType.PROJECT_ADD, main.participants.getParticipantUsers(parent), arrayOf(
								parent,
								project
							)
						)
					}
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