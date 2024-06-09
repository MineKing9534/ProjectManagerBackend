package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Where
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main
import java.time.Instant

data class Project(
	@Transient override val main: Main,
	@Column(key = true) override val id: ID = DEFAULT_ID,
	@Column(unique = true) override val name: String = "",
	@Column override val parent: String = ""
) : DataClass<Project>, Identifiable, MeetingResource, Comparable<Project> {
	override val resourceType = ResourceType.PROJECT
	override fun getTable() = main.projects

	override fun compareTo(other: Project): Int = name.compareTo(other.name)
}

interface ProjectTable : ResourceTable<Project> {
	fun create(name: String, parent: Resource? = null, location: String, time: Instant): Project {
		val project = insert(Project(main, name = name, parent = if(parent == null) "" else "${ parent.resourceType }:${ parent.id.asString() }"))
		main.meetings.insert(Meeting(main = main, id = project.id,
			parent = "PROJECT:${ project.id.asString() }",
			name = project.name,
			type = MeetingType.EVENT,
			location = location,
			time = time
		))

		return project
	}

	fun getProjects(parent: Resource, order: Order? = null) = selectMany(Where.equals("parent", "${ parent.resourceType }:${ parent.id.asString() }"), order ?: ResourceTable.DEFAULT_ORDER)
	fun getProjectCount(parent: Resource) = getRowCount(Where.equals("parent", "${ parent.resourceType }:${ parent.id.asString() }"))

	override fun delete(id: String): Boolean {
		val result = super.delete(id)

		if (result) {
			main.meetings.selectMany(Where.equals("parent", "PROJECT:$id")).forEach { main.meetings.delete(it.id.asString()) }
		}

		return result
	}
}