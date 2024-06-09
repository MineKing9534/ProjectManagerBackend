package de.mineking.manager.api.endpoints

import de.mineking.databaseutils.exception.ConflictException
import de.mineking.manager.api.*
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.ResourceType
import de.mineking.manager.data.UserTable
import de.mineking.manager.main.EmailType
import de.mineking.manager.main.containsAny
import de.mineking.manager.main.hashPassword
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.bodyAsClass
import java.util.*

fun String.isValidName(): Boolean = matches("^[a-zA-ZÄäÖöÜüß-]{2,}$".toRegex())
fun String.isValidEmail(): Boolean = matches("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$".toRegex())

fun UserEndpoints() {
	get {
		with(it) {
			checkAuthorization(admin = true)

			val skills = queryParam("skills")?.split(",")?.toList()

			if (skills.isNullOrEmpty()) paginateResult(main.users.rowCount, main.users::getAll, UserTable.DEFAULT_ORDER)
			else {
				val condition = containsAny("skills", skills)
				paginateResult(main.users.getRowCount(condition), { main.users.selectMany(condition, it) }, UserTable.DEFAULT_ORDER)
			}
		}
	}

	post("csv") {
		with(it) {
			checkAuthorization(admin = true)

			result(main.users.exportCSV())

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
			data class UpdateRequest(val firstName: String?, val lastName: String?, val info: String?, val emailTypes: EnumSet<EmailType>?)

			val request = bodyAsClass<UpdateRequest>()

			val auth = checkAuthorization()
			val user = auth.user

			if (request.firstName != null && !request.firstName.isValidName()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (request.lastName != null && !request.lastName.isValidName()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (request.emailTypes != null && !request.emailTypes.all { it.custom }) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)

			user.copy(
				firstName = request.firstName ?: user.firstName,
				lastName = request.lastName ?: user.lastName,
				info = request.info ?: user.info,
				emailTypes = request.emailTypes ?: user.emailTypes
			).update()

			json(user)
		}
	}

	post("@me/reset-password") {
		with(it) {
			data class Response(val token: String)

			val auth = checkAuthorization()

			val token = main.authenticator.generatePasswordResetToken(auth.user.id.asString())

			json(Response(token))
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
				val teamMeetings = teams.flatMap { it.getMeetings() }

				val projects = teams.flatMap { it.getProjects() }.map { it.id.asString() } + main.participants.getParents(user.id, ResourceType.PROJECT)
				val projectMeetings = projects.flatMap { main.meetings.getMeetings(it, ResourceType.PROJECT) }

				val meetings = main.participants.getParents(user.id, ResourceType.MEETING)

				paginateResult(teamMeetings + projectMeetings + main.meetings.getByIds(meetings))
			}
		}

		get("projects") {
			with(it) {
				val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)

				val teams = main.teams.getUserTeams(user.id)
				val teamProjects = teams.flatMap { it.getProjects() }

				val projects = main.participants.getParents(user.id, ResourceType.PROJECT)

				paginateResult(teamProjects + main.projects.getByIds(projects))
			}
		}
	}
}