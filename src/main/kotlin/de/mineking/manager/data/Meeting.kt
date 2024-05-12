package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.javautils.ID
import de.mineking.manager.data.type.Identifiable
import de.mineking.manager.data.type.Resource
import de.mineking.manager.main.Main

data class Meeting(
	@Transient val main: Main,
	@Column(key = true) override val id: ID? = null,
	@Column override val name: String = "",
	@Column val description: String = "",
	@Column val type: MeetingType = MeetingType.MEETING
) : DataClass<Meeting>, Identifiable, Resource {
	override val resourceType = ParentType.TEAM
	override fun getTable() = main.meetings
}
