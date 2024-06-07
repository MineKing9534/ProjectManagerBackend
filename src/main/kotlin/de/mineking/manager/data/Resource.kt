package de.mineking.manager.data

import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Where
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.main.Main
import java.io.File

interface Resource : Identifiable {
	val main: Main

	val name: String
	val resourceType: ResourceType
	val parent: String

	fun getParticipants(recursive: Boolean = true): Collection<String> = main.participants.getParticipantUsers(this, recursive)

	fun resolveParticipants(order: Order? = null, where: Where? = null) = main.users.getByIds(getParticipants(false), order, where)
	fun getParticipantCount(recursive: Boolean = true, where: Where? = null): Int {
		return if (where == null) getParticipants(recursive).size
		else resolveParticipants(null, where).size
	}
}

enum class ResourceType(val error: ErrorResponseType, val table: (main: Main) -> ResourceTable<*>) {
	TEAM(ErrorResponseType.TEAM_NOT_FOUND, { it.teams }),
	MEETING(ErrorResponseType.MEETING_NOT_FOUND, { it.meetings })
}

interface ResourceTable<T : Resource> : IdentifiableTable<T> {
	companion object {
		val DEFAULT_ORDER = Order.ascendingBy("name")
	}

	override fun delete(id: String): Boolean {
		main.participants.delete(Where.equals("parent", id))
		File("files/$id").deleteRecursively()

		return super.delete(id)
	}
}