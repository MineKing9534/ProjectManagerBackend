package de.mineking.manager.api.endpoints

import de.mineking.manager.api.TokenType
import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.api.paginateResult
import de.mineking.manager.data.MemberType
import de.mineking.manager.data.ParentType
import de.mineking.manager.data.table.UserTable
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass

fun MembersEndpoints() {
	path("users") {
		get {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				val type = attribute<ParentType>("type")!!
				type.table(main).getById(id) ?: throw ErrorResponse(type.error)

				paginateResult(this, type.table(main).rowCount, { order -> main.participants.getParticipantUsers(id, false, order) }, UserTable.DEFAULT_ORDER)
			}
		}

		put("@me") {
			with(it) {
				val auth = checkAuthorization()

				val id = pathParam("id")
				val type = attribute<ParentType>("type")!!
				val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

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
				val type = attribute<ParentType>("type")!!
				val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

				json(main.teams.getTeams(resource.id!!.asString()))
			}
		}

		put("{member}") {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				val type = attribute<ParentType>("type")!!
				val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

				val memberId = pathParam("member")
				val member = main.teams.getById(memberId) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

				main.participants.join(member.id!!, MemberType.TEAM, resource.id!!, attribute("type")!!)
			}
		}

		delete("{member}") {
			with(it) {
				checkAuthorization(admin = true)

				val id = pathParam("id")
				val type = attribute<ParentType>("type")!!
				val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

				val memberId = pathParam("member")
				val member = main.teams.getById(memberId) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

				main.participants.leave(member.id!!, resource.id!!)
			}
		}
	}
}