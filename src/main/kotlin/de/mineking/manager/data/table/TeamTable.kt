package de.mineking.manager.data.table

import de.mineking.databaseutils.Where
import de.mineking.manager.data.MemberType
import de.mineking.manager.data.ParentType
import de.mineking.manager.data.Team

interface TeamTable : ResourceTable<Team> {
	fun create(name: String): Team = insert(Team(main, name = name))

	fun getTeams(parent: String) = getByIds(getTeamIds(parent), ResourceTable.DEFAULT_ORDER)
	fun getTeamIds(parent: String): List<String> {
		val parents = main.participants.getParents(parent)
			.filter { it.parentType == ParentType.TEAM }
			.map { it.parent.asString() }

		return parents + parents.flatMap { getChildren(it) }
	}

	private fun getChildren(parent: String): List<String> {
		val children = main.participants.selectMany(Where.equals("parent", parent))
			.filter { it.memberType == MemberType.TEAM }
			.map { it.member.asString() }

		if (children.isEmpty()) return emptyList()

		return (children + children.flatMap { getChildren(it) })
	}
}