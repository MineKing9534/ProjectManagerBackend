package de.mineking.manager.api.endpoints

import de.mineking.databaseutils.exception.ConflictException
import de.mineking.javautils.ID
import de.mineking.manager.api.*
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.MemberType
import de.mineking.manager.data.ParentType
import de.mineking.manager.main.EmailClient
import de.mineking.manager.main.hashPassword
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass

fun UserEndpoints() {
	get {
		with(it) {
			checkAuthorization(admin = true)
			json(main.users.getAllIds())
		}
	}

	post("csv") {
		with(it) {
			checkAuthorization(admin = true)
			result(main.users.exportCSV())
				.header("content-type", "csv")
				.header("content-disposition", "inline; filename=\"Nutzerliste.csv\"");
		}
	}

	post("resolve") {
		with(it) {
			checkAuthorization(admin = true)

			data class Request(val ids: List<String>)

			val request = bodyAsClass<Request>()

			json(main.users.getByIds(request.ids))
		}
	}

	post {
		with(it) {
			val auth = checkAuthorization(type = TokenType.INVITE)

			data class Request(val firstName: String, val lastName: String, val email: String)

			val request = bodyAsClass<Request>()

			if (main.users.getByEmail(request.email) != null) throw ErrorResponse(ErrorResponseType.USER_ALREADY_EXISTS)

			if (request.firstName.length < 2) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (request.lastName.length < 2) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (!EmailClient.isValidEmail(request.email)) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)

			val id = auth.jwt.subject!!
			val type = auth.jwt.getClaim("type")!!.asString()

			TODO("EMAIL verification")
		}
	}

	post("verify") {
		with(it) {
			val auth = checkAuthorization(type = TokenType.EMAIL)

			data class Request(val password: String)

			val request = bodyAsClass<Request>()

			val email = auth.jwt.subject
			val firstName = auth.jwt.getClaim("firstName")!!.asString()
			val lastName = auth.jwt.getClaim("lastName")!!.asString()
			val password = request.password

			if (password.length < 8) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)

			if (main.users.getByEmail(email) != null) throw ErrorResponse(ErrorResponseType.USER_ALREADY_EXISTS)

			main.users.create(firstName, lastName, email, hashPassword(password))
		}
	}

	put {
		with(it) {
			checkSuperUser()

			data class Request(val firstName: String, val lastName: String, val email: String, val password: String)

			val request = bodyAsClass<Request>()

			try {
				main.users.create(request.firstName, request.lastName, request.email, hashPassword(request.password))
			} catch (_: ConflictException) {
				throw ErrorResponse(ErrorResponseType.USER_ALREADY_EXISTS)
			}
		}
	}

	path("{id}") {
		get {
			with(it) {
				val auth = checkAuthorization()

				var id = pathParam("id")
				if (id == "@me") id = auth.user.id!!.asString()

				val known = main.participants.getParents(auth.user.id!!.asString()).flatMap { main.participants.getParticipantUserIds(it.parent.asString()) }
				if (!auth.user.admin && auth.user.id!!.asString() != id && auth.user.id!!.asString() !in known) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

				val user = main.users.getById(id) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)
				json(user)
			}
		}

		delete {
			with(it) {
				val target = getTarget()
				if (!main.users.delete(target)) throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)
			}
		}

		patch {
			with(it) {
				data class UpdateRequest(val firstName: String?, val lastName: String?)

				val request = bodyAsClass<UpdateRequest>()

				val target = getTarget()
				val user = main.users.getById(target) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)

				user.copy(
					firstName = request.firstName ?: user.firstName,
					lastName = request.lastName ?: user.lastName
				).update()
			}
		}

		path("skills") {
			get {
				with(it) {
					val target = getTarget()
					val user = main.users.getById(target) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)

					json(user.skills)
				}
			}

			put {
				with(it) {
					data class Request(val skills: List<String>)

					val request = bodyAsClass<Request>()

					val target = getTarget()
					val user = main.users.getById(target) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)

					val skills = main.skills.getAllIds()
					if (!skills.containsAll(request.skills)) throw ErrorResponse(ErrorResponseType.SKILL_NOT_FOUND)

					user.copy(skills = request.skills).update()
					json(request.skills)
				}
			}

			post {
				with(it) {
					data class Request(val id: String)

					val request = bodyAsClass<Request>()

					val target = getTarget()
					val user = main.users.getById(target) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)

					if (main.skills.getById(request.id) == null) throw ErrorResponse(ErrorResponseType.SKILL_NOT_FOUND)

					user.copy(skills = user.skills + request.id).update()
				}
			}

			delete("{skill}") {
				with(it) {
					val target = getTarget()
					val user = main.users.getById(target) ?: throw ErrorResponse(ErrorResponseType.USER_NOT_FOUND)

					val skill = pathParam("skill")
					user.copy(skills = user.skills - skill).update()
				}
			}
		}

		path("teams") {
			get {
				with(it) {
					val target = getTarget()
					json(main.teams.getTeams(target))
				}
			}

			put("{team}") {
				with(it) {
					val target = getTarget()

					val id = pathParam("team")
					val meeting = main.teams.getById(id) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

					main.participants.join(ID.decode(target), MemberType.USER, meeting.id!!, ParentType.TEAM)
				}
			}

			delete("{team}") {
				with(it) {
					val target = getTarget()

					val id = pathParam("team")
					val meeting = main.teams.getById(id) ?: throw ErrorResponse(ErrorResponseType.TEAM_NOT_FOUND)

					if (!main.participants.leave(ID.decode(target), meeting.id!!)) throw ErrorResponse(ErrorResponseType.PARTICIPANT_NOT_FOUND)
				}
			}
		}

		path("meetings") {
			get {
				with(it) {
					val target = getTarget()
					json(main.meetings.getMeetings(target))
				}
			}

			put("{meeting}") {
				with(it) {
					val target = getTarget()

					val id = pathParam("meeting")
					val meeting = main.meetings.getById(id) ?: throw ErrorResponse(ErrorResponseType.MEETING_NOT_FOUND)

					main.participants.join(ID.decode(target), MemberType.USER, meeting.id!!, ParentType.MEETING)
				}
			}

			delete("{meeting}") {
				with(it) {
					val target = getTarget()

					val id = pathParam("meeting")
					val meeting = main.meetings.getById(id) ?: throw ErrorResponse(ErrorResponseType.MEETING_NOT_FOUND)

					if (!main.participants.leave(ID.decode(target), meeting.id!!)) throw ErrorResponse(ErrorResponseType.PARTICIPANT_NOT_FOUND)
				}
			}
		}
	}
}