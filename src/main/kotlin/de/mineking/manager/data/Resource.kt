package de.mineking.manager.data

import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Where
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.MeetingTable.Companion.DEFAULT_ORDER
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

	fun getParticipants(recursive: Boolean = true): Collection<String> = main.participants.getParticipantUsers(this, recursive)

	fun resolveParticipants(order: Order? = null, where: Where? = null) = main.users.getByIds(getParticipants(false), order, where)
	fun getParticipantCount(recursive: Boolean = true, where: Where? = null): Int {
		return if (where == null) getParticipants(recursive).size
		else resolveParticipants(null, where).size
	}
}

interface MeetingResource : Resource {
	fun getMeetings(order: Order? = null) = main.meetings.getMeetings(id.asString(), resourceType, order)
	fun getMeetingCount() = main.meetings.getMeetingCount(this)
}

enum class ResourceType(val error: ErrorResponseType, val table: (main: Main) -> ResourceTable<*>) {
	TEAM(ErrorResponseType.TEAM_NOT_FOUND, { it.teams }),
	PROJECT(ErrorResponseType.PROJECT_NOT_FOUND, { it.projects }),
	MEETING(ErrorResponseType.MEETING_NOT_FOUND, { it.meetings })
}

interface ResourceTable<T : Resource> : IdentifiableTable<T> {
	companion object {
		val DEFAULT_ORDER = Order.ascendingBy("name")
	}

	override fun delete(id: String): Boolean {
		val result = super.delete(id)

		if (result) {
			main.participants.delete(Where.equals("parent", id))
			File("files/$id").deleteRecursively()
		}

		return result
	}
}