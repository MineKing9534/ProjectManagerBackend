package de.mineking.manager.api.endpoints

import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.data.MeetingType
import de.mineking.manager.data.ParentType
import de.mineking.manager.data.table.MeetingTable
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass
import java.time.Instant

fun MeetingEndpoints() {
	get {
		with(it) {
			checkAuthorization(admin = true)
			json(main.meetings.getAll(MeetingTable.DEFAULT_ORDER))
		}
	}

	post {
		with(it) {
			checkAuthorization(admin = true)

			data class Request(val name: String, val time: Instant, val location: String, val type: MeetingType)

			val request = bodyAsClass<Request>()

			json(main.meetings.create(request.name, request.time, request.location, request.type))
		}
	}

	path("{id}") {
		before {
			with(it) {
				attribute("type", ParentType.MEETING)
			}
		}

		path("files", ::FileEndpoints)
		path("members", ::MembersEndpoints)

		get {
			with(it) {
				val auth = checkAuthorization()

				val id = pathParam("id")
				val meeting = main.meetings.getById(id) ?: throw ErrorResponse(ErrorResponseType.MEETING_NOT_FOUND)

				if (!auth.user.admin && auth.user.id!!.asString() !in main.participants.getParticipantUserIds(id)) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

				json(meeting)
			}
		}

		delete {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				if (!main.meetings.delete(id)) throw ErrorResponse(ErrorResponseType.MEETING_NOT_FOUND)
			}
		}

		patch {
			with(it) {
				checkAuthorization(admin = true)

				data class Request(val name: String?, val location: String?, val time: Instant?)

				val request = bodyAsClass<Request>()

				val id = pathParam("id")
				val meeting = main.meetings.getById(id) ?: throw ErrorResponse(ErrorResponseType.MEETING_NOT_FOUND)

				json(meeting.copy(
					name = request.name ?: meeting.name,
					location = request.location ?: meeting.location,
					time = request.time ?: meeting.time
				).update())
			}
		}
	}
}