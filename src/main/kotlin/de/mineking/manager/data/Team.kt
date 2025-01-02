package de.mineking.manager.data

import de.mineking.database.*
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main

data class Team(
	@Transient override val main: Main,
	@Key @Column override val id: ID = DEFAULT_ID,
	@Unique @Column override val name: String = "",
	@Column override val parent: String = ""
) : DataObject<Team>, Identifiable, MeetingResource, Comparable<Team> {
	override val resourceType = ResourceType.TEAM
	@Transient override val table = main.teams

	fun getProjects(order: Order? = null, offset: Int? = null, limit: Int? = null) = main.projects.getProjects(this, order, offset, limit)
	fun getProjectCount() = main.projects.getProjectCount(this)

	fun getAccessibleTeams(): Collection<Team> {
		val children = table.getChildren(this)

		return if (children.isEmpty()) emptyList()
		else children.flatMap { it.getAccessibleTeams() + it }
	}

	override fun compareTo(other: Team): Int = name.compareTo(other.name)
}

interface TeamTable : ResourceTable<Team> {
	@Insert fun create(@Parameter name: String): UpdateResult<Team>

	override fun delete(id: String): Boolean {
		val result = super.delete(id)

		if (result) {
			selectValue(property(Team::id), where = property(Team::parent) isEqualTo value("TEAM:$id")).stream().forEach { delete(it.asString()) }
			main.projects.selectValue(property(Project::id), where = property(Project::parent) isEqualTo value("TEAM:$id")).stream().forEach { main.projects.delete(it.asString()) }
			main.meetings.selectValue(property(Meeting::id), where = property(Meeting::parent) isEqualTo value("TEAM:$id")).stream().forEach { main.meetings.delete(it.asString()) }
		}

		return result
	}

	fun getChildren(parent: Resource) = select(where = property(Team::parent) isEqualTo value("${ parent.resourceType }:${ parent.id.asString() }")).list()

	fun getUserTeams(user: ID): Collection<Team> = main.participants.getParents(user, ResourceType.TEAM)
		.mapNotNull { getById(it) }
		.flatMap { it.getAccessibleTeams() + it }
}