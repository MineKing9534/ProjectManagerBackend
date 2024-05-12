package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.javautils.ID
import de.mineking.manager.data.type.Identifiable
import de.mineking.manager.data.type.Resource
import de.mineking.manager.main.Main

data class Team(
	@Transient val main: Main,
	@Column(key = true) override val id: ID? = null,
	@Column(unique = true) override val name: String = ""
) : DataClass<Team>, Identifiable, Resource {
	override val resourceType = ParentType.TEAM
	override fun getTable() = main.teams
}
