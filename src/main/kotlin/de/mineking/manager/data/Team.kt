package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Where
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main

data class Team(
	@Transient override val main: Main,
	@Column(key = true) override val id: ID = DEFAULT_ID,
	@Column(unique = true) override val name: String = "",
	@Column override val parent: String = ""
) : DataClass<Team>, Identifiable, Resource, Comparable<Team> {
	override val resourceType = ResourceType.TEAM
	override fun getTable() = main.teams

	fun getMeetings(order: Order? = null) = main.meetings.getMeetings(id.asString(), order)
	fun getMeetingCount() = main.meetings.getRowCount(Where.equals("parent", this.id.asString()))

	fun getAccessibleTeams(): Collection<Team> {
		val children = table.getChildren(id)

		return if(children.isEmpty()) emptyList()
		else children.flatMap { it.getAccessibleTeams() + it }
	}

	override fun compareTo(other: Team): Int = name.compareTo(other.name)
}

interface TeamTable : ResourceTable<Team> {
	fun create(name: String): Team = insert(Team(main, name = name))

	fun getChildren(parent: ID) = selectMany(Where.equals("parent", parent))

	fun getUserTeams(user: ID): Collection<Team> = main.participants.getParents(user, ResourceType.TEAM)
		.mapNotNull { getById(it) }
		.flatMap { it.getAccessibleTeams() }
}