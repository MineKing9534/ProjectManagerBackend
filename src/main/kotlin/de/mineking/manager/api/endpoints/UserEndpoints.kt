package de.mineking.manager.api.endpoints

import de.mineking.databaseutils.Where
import de.mineking.databaseutils.exception.ConflictException
import de.mineking.manager.api.*
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.MeetingTable
import de.mineking.manager.data.UserTable
import de.mineking.manager.data.ResourceType
import de.mineking.manager.main.EmailType
import de.mineking.manager.main.hashPassword
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass
import java.util.*

fun String.isValidName(): Boolean = matches("^[a-zA-Zäöüß]{2,}$".toRegex())
fun String.isValidEmail(): Boolean = matches("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$".toRegex())

fun UserEndpoints() {
	get {
		with(it) {
			checkAuthorization(admin = true)

			paginateResult(main.users.rowCount, main.users::getAll, UserTable.DEFAULT_ORDER)
		}
	}

	post("csv") {
		with(it) {
			println("a")

			checkAuthorization(admin = true)

			println("b")

			result(main.users.exportCSV())

			println("c")

			header("content-type", "csv")
			header("content-disposition", "inline; filename=\"Nutzerliste.csv\"")
		}
	}

	post {
		with(it) {
			val auth = checkAuthorization(type = TokenType.INVITE)

			data class Request(val firstName: String, val lastName: String, val email: String)

			val request = bodyAsClass<Request>()

			if (main.users.getByEmail(request.email) != null) throw ErrorResponse(ErrorResponseType.USER_ALREADY_EXISTS)

			if (!request.firstName.isValidName()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (!request.lastName.isValidName()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (!request.email.isValidEmail()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)

			val id = auth.jwt.subject!!
			val type = ResourceType.valueOf(auth.jwt.getClaim("type").asString())

			val parent = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

			main.email.sendVerificationEmail(request.firstName, request.lastName, request.email, parent)
		}
	}

	post("verify") {
		with(it) {
			val auth = checkAuthorization(type = TokenType.EMAIL)

			data class Request(val password: String)

			val request = bodyAsClass<Request>()

			val email = auth.jwt.subject
			val firstName = auth.jwt.getClaim("fn").asString()
			val lastName = auth.jwt.getClaim("ln").asString()
			val password = request.password

			if (password.length < 8) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (main.users.getByEmail(email) != null) throw ErrorResponse(ErrorResponseType.USER_ALREADY_EXISTS)

			val id = auth.jwt.getClaim("pi").asString()
			val type = ResourceType.valueOf(auth.jwt.getClaim("pt").asString())

			val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

			val user = main.users.create(firstName, lastName, email, hashPassword(password))
			main.participants.join(user.id, resource.id, type)
		}
	}

	put {
		with(it) {
			checkSuperUser()

			data class Request(val firstName: String, val lastName: String, val email: String, val password: String)

			val request = bodyAsClass<Request>()

			try {
				json(main.users.create(request.firstName, request.lastName, request.email, hashPassword(request.password)))
			} catch (_: ConflictException) {
				throw ErrorResponse(ErrorResponseType.USER_ALREADY_EXISTS)
			}
		}
	}

	patch("@me") {
		with(it) {
			data class UpdateRequest(val firstName: String?, val lastName: String?, val emailTypes: EnumSet<EmailType>?)

			val request = bodyAsClass<UpdateRequest>()

			val auth = checkAuthorization()
			val user = auth.user

			if (request.firstName != null && !request.firstName.isValidName()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (request.lastName != null && !request.lastName.isValidName()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (request.emailTypes != null && !request.emailTypes.all { it.custom }) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)

			user.copy(
				firstName = request.firstName ?: user.firstName,
				lastName = request.lastName ?: user.lastName,
				emailTypes = request.emailTypes ?: user.emailTypes
			).update()

			json(user)
		}
	}

	path("{id}") {
		get {
			with(it) {
				val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)
				json(user)
			}
		}

		delete {
			with(it) {
				val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)
				user.delete()
			}
		}

		path("skills") {
			get {
				with(it) {
					val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)
					json(user.skills)
				}
			}

			put {
				with(it) {
					data class Request(val skills: List<String>)

					val request = bodyAsClass<Request>()

					val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)

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

					val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)

					if (main.skills.getById(request.id) == null) throw ErrorResponse(ErrorResponseType.SKILL_NOT_FOUND)

					user.copy(skills = user.skills + request.id).update()
				}
			}

			delete("{skill}") {
				with(it) {
					val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)

					val skill = pathParam("skill")
					user.copy(skills = user.skills - skill).update()
				}
			}
		}

		get("teams") {
			with(it) {
				val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)
				paginateResult(main.teams.getUserTeams(user.id))
			}
		}

		get("meetings") {
			with(it) {
				val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)

				val teams = main.teams.getUserTeams(user.id)
				val teamMeetings = teams.flatMap { main.meetings.getMeetings(it.id.asString()) }

				val meetings = main.participants.getParents(user.id, ResourceType.MEETING)

				paginateResult(teamMeetings + main.meetings.getByIds(meetings))
			}
		}
	}
}