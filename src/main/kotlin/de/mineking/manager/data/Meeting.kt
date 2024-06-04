package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.javautils.ID
import de.mineking.manager.data.type.Identifiable
import de.mineking.manager.data.type.Resource
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main
import java.time.Instant

data class Meeting(
	@Transient val main: Main,
	@Column(key = true) override val id: ID? = null,
	@Column val parent: ID = DEFAULT_ID,
	@Column override val name: String = "",
	@Column val type: MeetingType = MeetingType.MEETING,
	@Column val location: String = "",
	@Column val time: Instant = Instant.now()
) : DataClass<Meeting>, Identifiable, Resource {
	override val resourceType = ParentType.TEAM
	override fun getTable() = main.meetings
}

enum class MeetingType {
	EVENT,
	PRACTICE,
	MEETING
}