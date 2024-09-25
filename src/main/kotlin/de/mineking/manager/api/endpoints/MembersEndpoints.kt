package de.mineking.manager.api.endpoints

import de.mineking.databaseutils.Where
import de.mineking.manager.api.*
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.Resource
import de.mineking.manager.data.ResourceType
import de.mineking.manager.data.UserTable
import de.mineking.manager.main.containsAny
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass

fun MembersEndpoints() {
	before {
		with(it) {
			val id = pathParam("id")
			val type = attribute<ResourceType>("type")!!
			val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

			attribute("resource", resource)
		}
	}

	get {
		with(it) {
			checkAuthorization(admin = true)

			val resource = attribute<Resource>("resource")!!
			val skills = queryParam("skills")?.split(",")?.toList()

			if (skills.isNullOrEmpty()) paginateResult(resource.getParticipantCount(false), resource::resolveParticipants, UserTable.DEFAULT_ORDER)
			else {
				val condition = containsAny("skills", skills)
				paginateResult(resource.getParticipantCount(false, condition), { resource.resolveParticipants(it, condition) }, UserTable.DEFAULT_ORDER)
			}
		}
	}

	post("csv") {
		with(it) {
			checkAuthorization(admin = true)

			val resource = attribute<Resource>("resource")!!

			result(main.users.exportCSV(Where.valueContainsField("id", main.participants.getParticipantUsers(resource))))
			header("content-type", "csv")
			header("content-disposition", "inline; filename=\"Nutzerliste_${resource.name}.csv\"")
		}
	}

	put("@me") {
		with(it) {
			val resource = attribute<Resource>("resource")!!

			val auth = checkAuthorization()

			if (!auth.user.admin) {
				data class Request(val invite: String)

				val request = bodyAsClass<Request>()
				val token = request.invite

				val invite = main.authenticator.checkAuthorization(token, type = TokenType.INVITE)
				val target = invite.jwt.subject

				if (target != resource.id.asString()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			}

			main.participants.join(auth.user.id, resource.id, attribute("type")!!)
		}
	}

	get("@me") {
		with(it) {
			val auth = checkAuthorization()

			val resource = attribute<Resource>("resource")!!

			main.participants.get(auth.user.id, resource.id) ?: throw ErrorResponse(ErrorResponseType.PARTICIPANT_NOT_FOUND)
		}
	}

	delete("{member}") {
		with(it) {
			val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND, "member")
			val resource = attribute<Resource>("resource")!!

			if (!main.participants.leave(user.id, resource.id)) throw ErrorResponse(ErrorResponseType.PARTICIPANT_NOT_FOUND)
		}
	}
}