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
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass
import java.time.Instant

fun TeamEndpoints() {
	get {
		with(it) {
			checkAuthorization(admin = true)

			paginateResult(main.teams.rowCount, main.teams::getAll, ResourceTable.DEFAULT_ORDER)
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
				attribute("type", ResourceType.TEAM)
			}
		}

		path("files", ::FileEndpoints)
		path("users", ::MembersEndpoints)

		get {
			with(it) {
				val auth = checkAuthorization()

				val id = pathParam("id")
				val team = main.teams.getById(id) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

				if (!auth.user.admin && auth.user.id.asString() !in main.participants.getParticipantUsers(team)) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

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

				data class Request(val name: String?, val parent: String?)

				val request = bodyAsClass<Request>()

				val id = pathParam("id")
				val team = main.teams.getById(id) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

				try {
					json(
						team.copy(
							name = request.name ?: team.name,
							parent = if (request.parent != null) {
								if(request.parent.isEmpty()) ""
								else {
									val temp = main.teams.getById(request.parent) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)
									if (temp.id.asString() == team.id.asString()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)

									temp.id.asString()
								}
							} else team.parent
						).update()
					)
				} catch (_: ConflictException) {
					throw ErrorResponse(ErrorResponseType.TEAM_ALREADY_EXISTS)
				}
			}
		}

		post("invites") {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				val team = main.teams.getById(id) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

				data class Response(val token: String)
				json(Response(main.authenticator.generateInviteToken(team)))
			}
		}

		path("meetings") {
			get {
				with(it) {
					val auth = checkAuthorization()

					val id = pathParam("id")
					val team = main.teams.getById(id) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

					if (!auth.user.admin && auth.user.id.asString() !in main.participants.getParticipantUsers(team)) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

					paginateResult(team.getMeetingCount(), team::getMeetings, MeetingTable.DEFAULT_ORDER)
				}
			}

			post {
				with(it) {
					checkAuthorization(admin = true)

					data class Request(val name: String, val time: Instant, val location: String, val type: MeetingType)

					val request = bodyAsClass<Request>()

					val id = pathParam("id")
					val team = main.teams.getById(id) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

					json(main.meetings.create(team.id.asString(), request.name, request.time, request.location, request.type))
				}
			}
		}
	}
}