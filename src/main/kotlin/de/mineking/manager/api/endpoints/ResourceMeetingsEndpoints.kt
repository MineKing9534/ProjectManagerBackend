package de.mineking.manager.api.endpoints

import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.api.paginateResult
import de.mineking.manager.data.*
import de.mineking.manager.main.EmailType
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass
import java.time.Instant

fun ResourceMeetingsEndpoints() {
	before {
		with(it) {
			val auth = checkAuthorization()

			val id = pathParam("id")
			val type = attribute<ResourceType>("type")!!
			val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

			if (!auth.user.admin && !resource.canBeAccessed(auth.user.id.asString())) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

			attribute("resource", resource)
		}
	}

	get {
		with(it) {
			val resource = attribute<MeetingResource>("resource")!!

			paginateResult(resource.getMeetingCount(), resource::getMeetings, MeetingTable.DEFAULT_ORDER)
		}
	}

	post {
		with(it) {
			checkAuthorization(admin = true)

			data class Request(val name: String, val time: Instant, val location: String, val type: MeetingType)

			val request = bodyAsClass<Request>()

			val resource = attribute<Resource>("resource")!!

			val meeting = main.meetings.create(resource,
				name = request.name,
				type = request.type,
				time = request.time,
				location = request.location
			)

			main.email.sendEmail(
				EmailType.MEETING_CREATE, main.participants.getParticipantUsers(resource), arrayOf(
					resource,
					meeting
				)
			)

			json(meeting)
		}
	}
}