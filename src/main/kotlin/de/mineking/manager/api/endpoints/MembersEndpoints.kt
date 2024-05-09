package de.mineking.manager.api.endpoints

import de.mineking.manager.api.TokenType
import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.data.MemberType
import de.mineking.manager.data.table.IdentifiableTable
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass

fun MembersEndpoints() {
	path("users") {
		get {
			with(it) {
				val auth = checkAuthorization()

				val id = pathParam("id")
				attribute<IdentifiableTable<*>>("table")?.getById(id) ?: throw ErrorResponse(attribute("error")!!)

				val users = main.participants.getParticipantUserIds(id)
				if (!auth.user.admin && auth.user.id!!.asString() !in users) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

				json(users)
			}
		}

		put("@me") {
			with(it) {
				val auth = checkAuthorization()

				val id = pathParam("id")
				val resource = attribute<IdentifiableTable<*>>("table")?.getById(id) ?: throw ErrorResponse(attribute("error")!!)

				data class Request(val invite: String)
				if (!auth.user.admin) {
					val request = bodyAsClass<Request>()
					val token = request.invite

					val invite = main.authenticator.checkAuthorization(token, type = TokenType.INVITE)
					val target = invite.jwt.subject

					if (target != id) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
				}

				main.participants.join(auth.user.id!!, MemberType.USER, resource.id!!, attribute("type")!!)
			}
		}
	}

	path("teams") {
		get {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				val resource = attribute<IdentifiableTable<*>>("table")?.getById(id) ?: throw ErrorResponse(attribute("error")!!)

				json(main.teams.getTeams(resource.id!!.asString()))
			}
		}

		put("{member}") {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				val resource = attribute<IdentifiableTable<*>>("table")?.getById(id) ?: throw ErrorResponse(attribute("error")!!)

				val memberId = pathParam("member")
				val member = main.teams.getById(memberId) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

				main.participants.join(member.id!!, MemberType.TEAM, resource.id!!, attribute("type")!!)
			}
		}

		delete("{member}") {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				val resource = attribute<IdentifiableTable<*>>("table")?.getById(id) ?: throw ErrorResponse(attribute("error")!!)

				val memberId = pathParam("member")
				val member = main.teams.getById(memberId) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

				main.participants.leave(member.id!!, resource.id!!)
			}
		}
	}
}