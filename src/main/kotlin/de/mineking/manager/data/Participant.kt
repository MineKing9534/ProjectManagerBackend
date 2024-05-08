package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main

data class Participant(
	@Transient val main: Main,
	@Column(key = true) val member: ID = DEFAULT_ID,
	@Column val memberType: MemberType = MemberType.USER,
	@Column(key = true) val parent: ID = DEFAULT_ID,
	@Column val parentType: ParentType = ParentType.TEAM
) : DataClass<Participant> {
	override fun getTable() = main.participants
}

enum class MemberType {
	USER,
	TEAM
}

enum class ParentType {
	TEAM,
	MEETING
}
