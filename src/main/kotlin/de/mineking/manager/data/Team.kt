package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Where
import de.mineking.javautils.ID
import de.mineking.manager.data.MeetingTable.Companion.DEFAULT_ORDER
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main

data class Team(
	@Transient override val main: Main,
	@Column(key = true) override val id: ID = DEFAULT_ID,
	@Column(unique = true) override val name: String = "",
	@Column override val parent: String = ""
) : DataClass<Team>, Identifiable, MeetingResource, Comparable<Team> {
	override val resourceType = ResourceType.TEAM
	override fun getTable() = main.teams

	fun getProjects(order: Order? = null) = main.projects.getProjects(this, order)
	fun getProjectCount() = main.projects.getProjectCount(this)

	fun getAccessibleTeams(): Collection<Team> {
		val children = table.getChildren(this)

		return if (children.isEmpty()) emptyList()
		else children.flatMap { it.getAccessibleTeams() + it }
	}

	override fun compareTo(other: Team): Int = name.compareTo(other.name)
}

interface TeamTable : ResourceTable<Team> {
	fun create(name: String): Team = insert(Team(main, name = name))

	override fun delete(id: String): Boolean {
		val result = super.delete(id)

		if (result) {
			selectMany(Where.equals("parent", "TEAM:$id")).forEach { delete(it.id.asString()) }
			main.projects.selectMany(Where.equals("parent", "TEAM:$id")).forEach { main.projects.delete(it.id.asString()) }
			main.meetings.selectMany(Where.equals("parent", "TEAM:$id")).forEach { main.meetings.delete(it.id.asString()) }
		}

		return result
	}

	fun getChildren(parent: Resource) = selectMany(Where.equals("parent", "${ parent.resourceType }:${ parent.id.asString() }"))

	fun getUserTeams(user: ID): Collection<Team> = main.participants.getParents(user, ResourceType.TEAM)
		.mapNotNull { getById(it) }
		.flatMap { it.getAccessibleTeams() + it }
}