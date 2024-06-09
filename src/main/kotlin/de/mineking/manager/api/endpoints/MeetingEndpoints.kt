package de.mineking.manager.api.endpoints

import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.api.paginateResult
import de.mineking.manager.data.MeetingTable
import de.mineking.manager.data.MeetingType
import de.mineking.manager.data.ResourceType
import de.mineking.manager.main.EmailType
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass
import java.time.Instant

fun MeetingEndpoints() {
	get {
		with(it) {
			checkAuthorization(admin = true)

			paginateResult(main.meetings.rowCount, main.meetings::getAll, MeetingTable.DEFAULT_ORDER)
		}
	}

	path("{id}") {
		before {
			with(it) {
				attribute("type", ResourceType.MEETING)
			}
		}

		path("files", ::FileEndpoints)
		path("users", ::MembersEndpoints)

		get {
			with(it) {
				val auth = checkAuthorization()

				val id = pathParam("id")
				val meeting = main.meetings.getById(id) ?: throw ErrorResponse(ErrorResponseType.MEETING_NOT_FOUND)

				if (!auth.user.admin && !meeting.canBeAccessed(auth.user.id.asString())) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

				json(meeting)
			}
		}

		delete {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")

				val meeting = main.meetings.getById(id) ?: throw ErrorResponse(ErrorResponseType.MEETING_NOT_FOUND)
				val resource = meeting.getParent() ?: throw ErrorResponse(ErrorResponseType.UNKNOWN)

				if (meeting.parent.contains(id)) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)

				main.email.sendEmail(
					EmailType.MEETING_DELETE, main.participants.getParticipantUsers(meeting), arrayOf(
						resource,
						meeting
					)
				)

				if (!main.meetings.delete(meeting.id.asString())) throw ErrorResponse(ErrorResponseType.MEETING_NOT_FOUND)
			}
		}

		patch {
			with(it) {
				checkAuthorization(admin = true)

				data class Request(val name: String?, val location: String?, val time: Instant?, val type: MeetingType?)

				val request = bodyAsClass<Request>()

				val id = pathParam("id")
				val meeting = main.meetings.getById(id) ?: throw ErrorResponse(ErrorResponseType.MEETING_NOT_FOUND)

				if (meeting.parent.contains(id)) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)

				main.email.sendEmail(
					EmailType.MEETING_UPDATE, main.participants.getParticipantUsers(meeting), arrayOf(
						meeting
					)
				)

				json(
					meeting.copy(
						name = request.name ?: meeting.name,
						location = request.location ?: meeting.location,
						time = request.time ?: meeting.time,
						type = request.type ?: meeting.type
					).update()
				)
			}
		}

		post("invites") {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				val meeting = main.meetings.getById(id) ?: throw ErrorResponse(ErrorResponseType.MEETING_NOT_FOUND)

				data class Response(val token: String)
				json(Response(main.authenticator.generateInviteToken(meeting)))
			}
		}
	}
}