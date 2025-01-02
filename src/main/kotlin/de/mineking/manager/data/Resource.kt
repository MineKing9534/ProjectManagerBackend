package de.mineking.manager.data

import de.mineking.database.*
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.main.Main
import java.io.File

interface Resource : Identifiable {
	val main: Main

	val name: String
	val resourceType: ResourceType
	val parent: String

	fun getParent(): Resource? {
		if (parent.isEmpty()) return null

		val temp = parent.split(":")
		val table = ResourceType.valueOf(temp[0]).table(main)

		return table.getById(temp[1])
	}

	fun canBeAccessed(user: String): Boolean {
		val participants = getParticipants(false)

		if (user in participants) return true

		return if (parent.isNotEmpty()) getParent()!!.canBeAccessed(user)
		else false
	}

	fun getParticipants(recursive: Boolean = true): Set<String> = main.participants.getParticipantUsers(this, recursive)

	fun getParticipantCount(recursive: Boolean = true, where: Where? = null): Int {
		return if (where == null) getParticipants(recursive).size
		else resolveParticipants(where, null).size
	}

	fun resolveParticipants(where: Where? = null, order: Order? = null, offset: Int? = null, limit: Int? = null) = main.users.getByIds(
		getParticipants(false),
		where = where,
		order = order,
		offset = offset,
		limit = limit
	)
}

interface MeetingResource : Resource {
	fun getMeetingCount() = main.meetings.getMeetingCount(this)
	fun getMeetings(order: Order? = null, offset: Int? = null, limit: Int? = null) = main.meetings.getMeetings(
		parent = id.asString(),
		type = resourceType,
		order = order,
		offset = offset,
		limit = limit
	)
}

enum class ResourceType(val error: ErrorResponseType, val table: (main: Main) -> ResourceTable<*>) {
	TEAM(ErrorResponseType.TEAM_NOT_FOUND, { it.teams }),
	PROJECT(ErrorResponseType.PROJECT_NOT_FOUND, { it.projects }),
	MEETING(ErrorResponseType.MEETING_NOT_FOUND, { it.meetings })
}

interface ResourceTable<T : Resource> : IdentifiableTable<T> {
	companion object {
		val DEFAULT_ORDER = ascendingBy("name")
	}

	override fun delete(id: String): Boolean {
		val result = super.delete(id)

		if (result) {
			main.participants.delete(where = property(Participant::parent) isEqualTo value(id))
			File("files/$id").deleteRecursively()
		}

		return result
	}
}