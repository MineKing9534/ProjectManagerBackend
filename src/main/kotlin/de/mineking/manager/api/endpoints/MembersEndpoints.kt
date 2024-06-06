package de.mineking.manager.api.endpoints

import de.mineking.databaseutils.Where
import de.mineking.manager.api.*
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.ResourceType
import de.mineking.manager.data.UserTable
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass

fun MembersEndpoints() {
	get {
		with(it) {
			checkAuthorization(admin = true)

			val id = pathParam("id")
			val type = attribute<ResourceType>("type")!!
			val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

			paginateResult(resource.getParticipantCount(false), resource::resolveParticipants, UserTable.DEFAULT_ORDER)
		}
	}

	post("csv") {
		with(it) {
			checkAuthorization(admin = true)

			val id = pathParam("id")
			val type = attribute<ResourceType>("type")!!
			val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

			result(main.users.exportCSV(Where.valueContainsField("id", main.participants.getParticipantUsers(resource))))
			header("content-type", "csv")
			header("content-disposition", "inline; filename=\"Nutzerliste_${resource.name}.csv\"")
		}
	}

	put("@me") {
		with(it) {
			val id = pathParam("id")
			val type = attribute<ResourceType>("type")!!
			val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

			val auth = checkAuthorization()

			if (!auth.user.admin) {
				data class Request(val invite: String)

				val request = bodyAsClass<Request>()
				val token = request.invite

				val invite = main.authenticator.checkAuthorization(token, type = TokenType.INVITE)
				val target = invite.jwt.subject

				if (target != id) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			}

			main.participants.join(auth.user.id, resource.id, attribute("type")!!)
		}
	}

	delete("{member}") {
		with(it) {
			checkAuthorization(admin = true)

			val id = pathParam("id")
			val type = attribute<ResourceType>("type")!!
			val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

			val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND, "member")

			if(!main.participants.leave(user.id, resource.id)) throw ErrorResponse(ErrorResponseType.PARTICIPANT_NOT_FOUND)
		}
	}
}