package de.mineking.manager.data

import de.mineking.database.*
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main
import java.time.Instant

data class Project(
	@Transient override val main: Main,
	@Key @Column override val id: ID = DEFAULT_ID,
	@Unique @Column override val name: String = "",
	@Column override val parent: String = ""
) : DataObject<Project>, Identifiable, MeetingResource, Comparable<Project> {
	override val resourceType = ResourceType.PROJECT
	@Transient override val table = main.projects

	override fun compareTo(other: Project): Int = name.compareTo(other.name)
}

interface ProjectTable : ResourceTable<Project> {
	fun create(name: String, parent: Resource? = null, location: String, time: Instant): UpdateResult<Project> {
		val result = insert(Project(main, name = name, parent =
			if(parent == null) ""
			else "${ parent.resourceType }:${ parent.id.asString() }")
		)

		val project = result.value ?: return result

		main.meetings.insert(Meeting(main = main,
			id = project.id,
			parent = "PROJECT:${ project.id.asString() }",
			name = project.name,
			type = MeetingType.EVENT,
			location = location,
			time = time
		))

		return result
	}

	fun getProjectCount(parent: Resource) = selectRowCount(where = property(Project::parent) isEqualTo value("${ parent.resourceType }:${ parent.id.asString() }"))
	fun getProjects(parent: Resource, order: Order? = null, offset: Int? = null, limit: Int? = null) = select(
		where = property(Project::parent) isEqualTo value("${ parent.resourceType }:${ parent.id.asString() }"),
		order = order ?: ResourceTable.DEFAULT_ORDER,
		offset = offset,
		limit = limit
	)

	override fun delete(id: String): Boolean {
		val result = super.delete(id)

		if (result) {
			main.meetings.selectValue(property(Meeting::id), where = property(Meeting::parent) isEqualTo value("PROJECT:$id")).stream().forEach { main.meetings.delete(it.asString()) }
		}

		return result
	}
}