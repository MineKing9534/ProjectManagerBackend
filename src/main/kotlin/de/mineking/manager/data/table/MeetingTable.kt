package de.mineking.manager.data.table

import de.mineking.manager.data.Meeting
import de.mineking.manager.data.ParentType

interface MeetingTable : IdentifiableTable<Meeting> {
	fun getMeetings(parent: String): List<Meeting> = getByIds(getMeetingIds(parent))
	fun getMeetingIds(parent: String): List<String> = (
			main.participants.getParents(parent) +
			main.teams.getTeamIds(parent).flatMap { main.participants.getParents(it) }
	).filter { it.parentType == ParentType.MEETING }.map { it.parent.asString() }
}
