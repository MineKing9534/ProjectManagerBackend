package de.mineking.manager.data.table

import de.mineking.databaseutils.Order
import de.mineking.manager.data.Meeting
import de.mineking.manager.data.MeetingType
import de.mineking.manager.data.ParentType
import java.time.Instant

interface MeetingTable : ResourceTable<Meeting> {
	companion object {
		val DEFAULT_ORDER = Order.ascendingBy("time")
	}

	fun create(name: String, time: Instant, location: String, type: MeetingType): Meeting = insert(Meeting(main, name = name, time = time, location = location, type = type))

	fun getMeetings(parent: String): List<Meeting> = getByIds(getMeetingIds(parent), ResourceTable.DEFAULT_ORDER)
	fun getMeetingIds(parent: String): List<String> = (
			main.participants.getParents(parent) +
			main.teams.getTeamIds(parent).flatMap { main.participants.getParents(it) }
	).filter { it.parentType == ParentType.MEETING }.map { it.parent.asString() }
}
