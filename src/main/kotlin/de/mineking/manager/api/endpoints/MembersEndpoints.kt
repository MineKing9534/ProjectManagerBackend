package de.mineking.manager.api.endpoints

import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.data.MemberType
import de.mineking.manager.data.table.IdentifiableTable
import io.javalin.apibuilder.ApiBuilder.*

fun MembersEndpoints() {
	get("users") { with(it) {
		val auth = checkAuthorization()

		val id = pathParam("id")
		attribute<IdentifiableTable<*>>("table")?.getById(id) ?: throw ErrorResponse(attribute("error")!!)

		val users = main.participants.getParticipantUserIds(id)
		if (!auth.user.admin && auth.user.id!!.asString() !in users) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

		json(users)
	} }

	path("teams") {
		get { with(it) {
			val auth = checkAuthorization()

			val id = pathParam("id")
			val resource = attribute<IdentifiableTable<*>>("table")?.getById(id) ?: throw ErrorResponse(attribute("error")!!)

			if (!auth.user.admin && auth.user.id!!.asString() !in main.participants.getParticipantUserIds(id)) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

			json(main.teams.getTeams(resource.id!!.asString()))
		} }

		put("{member}") { with(it) {
			checkAuthorization(admin = true)

			val id = pathParam("id")
			val resource = attribute<IdentifiableTable<*>>("table")?.getById(id) ?: throw ErrorResponse(attribute("error")!!)

			val memberId = pathParam("member")
			val member = main.teams.getById(memberId) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

			main.participants.join(member.id!!, MemberType.TEAM, resource.id!!, attribute("parent")!!)
		} }

		delete("{member}") { with(it) {
			checkAuthorization(admin = true)

			val id = pathParam("id")
			val resource = attribute<IdentifiableTable<*>>("table")?.getById(id) ?: throw ErrorResponse(attribute("error")!!)

			val memberId = pathParam("member")
			val member = main.teams.getById(memberId) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

			main.participants.leave(member.id!!, resource.id!!)
		} }
	}
}