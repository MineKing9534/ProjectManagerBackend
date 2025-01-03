package de.mineking.manager.api.endpoints

import de.mineking.database.property
import de.mineking.database.value
import de.mineking.manager.api.*
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.ResourceType
import de.mineking.manager.data.User
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

			if (skills.isNullOrEmpty()) paginateResult(main.users.selectRowCount(), { order, offset, limit -> main.users.getAll(order = order, offset = offset, limit = limit) }, UserTable.DEFAULT_ORDER)
			else {
				val condition = property(User::skills) containsAny value(skills)
				paginateResult(main.users.selectRowCount(condition), { order, offset, limit -> main.users.select(where = condition, order = order, offset = offset, limit = limit).list() }, UserTable.DEFAULT_ORDER)
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

			val result = main.users.create(request.firstName, request.lastName, request.email, hashPassword(request.password))
			if (result.uniqueViolation) throw ErrorResponse(ErrorResponseType.USER_ALREADY_EXISTS)

			json(result.getOrThrow())
		}
	}

	patch("@me") {
		with(it) {
			data class UpdateRequest(val firstName: String?, val lastName: String?, val emailTypes: EnumSet<EmailType>?)

			val request = bodyAsClass<UpdateRequest>()

			val auth = checkAuthorization()
			val user = auth.user()

			if (request.firstName != null && !request.firstName.isValidName()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (request.lastName != null && !request.lastName.isValidName()) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			if (request.emailTypes != null && !request.emailTypes.all { it.custom }) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)

			val result = user.copy(
				firstName = request.firstName ?: user.firstName,
				lastName = request.lastName ?: user.lastName,
				emailTypes = request.emailTypes ?: user.emailTypes
			).update()

			if (result.uniqueViolation) throw ErrorResponse(ErrorResponseType.USER_ALREADY_EXISTS)
			json(result.getOrThrow())
		}
	}

	post("@me/reset-password") {
		with(it) {
			data class Response(val token: String)

			val auth = checkAuthorization()

			val token = main.authenticator.generatePasswordResetToken(auth.user().id.asString())

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

		path("inputs") {
			get {
				with(it) {
					val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)
					json(user.inputs)
				}
			}

			post {
				with(it) {
					val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)

					data class Request(val values: Map<String, Any>)

					val request = bodyAsClass<Request>()

					request.values.forEach { (id, value) ->
						val input = main.inputs.getById(id) ?: throw ErrorResponse(ErrorResponseType.INPUT_NOT_FOUND)
						if (!input.type.validate(value, input)) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
					}

					user.inputs.putAll(request.values)
					user.inputs.keys.retainAll(main.inputs.getAllIds())

					user.update()

					json(user.inputs)
				}
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
					data class Request(val skills: Set<String>)

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
				val teamMeetings = teams.flatMap { it.getMeetings().list() }

				val projects = teams.flatMap { it.getProjects().list() }.map { it.id.asString() } + main.participants.getParents(user.id, ResourceType.PROJECT)
				val projectMeetings = projects.flatMap { main.meetings.getMeetings(it, ResourceType.PROJECT).list() }

				val meetings = main.participants.getParents(user.id, ResourceType.MEETING)

				paginateResult(teamMeetings + projectMeetings + main.meetings.getByIds(meetings))
			}
		}

		get("projects") {
			with(it) {
				val user = getTarget(main.users, ErrorResponseType.USER_NOT_FOUND)

				val teams = main.teams.getUserTeams(user.id)
				val teamProjects = teams.flatMap { it.getProjects().list() }

				val projects = main.participants.getParents(user.id, ResourceType.PROJECT)

				paginateResult(teamProjects + main.projects.getByIds(projects))
			}
		}
	}
}