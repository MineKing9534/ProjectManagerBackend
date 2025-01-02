package de.mineking.manager.data

import de.mineking.database.*
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main
import java.time.Instant

data class Meeting(
	@Transient override val main: Main,
	@Key @Column override val id: ID = DEFAULT_ID,
	@Column override val parent: String = "",
	@Column override val name: String = "",
	@Column val type: MeetingType = MeetingType.MEETING,
	@Column val location: String = "",
	@Column val time: Instant = Instant.now()
) : DataObject<Meeting>, Identifiable, Resource, Comparable<Meeting> {
	override val resourceType = ResourceType.MEETING
	@Transient override val table = main.meetings

	override fun compareTo(other: Meeting): Int = Comparator.comparing(Meeting::time)
		.thenComparing(Meeting::name)
		.compare(this, other)
}

enum class MeetingType {
	EVENT,
	PRACTICE,
	MEETING
}

interface MeetingTable : ResourceTable<Meeting> {
	companion object {
		val DEFAULT_ORDER = ascendingBy("time")
	}

	fun create(
		parent: Resource,
		name: String,
		time: Instant,
		location: String,
		type: MeetingType
	): Meeting = insert(Meeting(
		main,
		parent = "${ parent.resourceType }:${ parent.id.asString() }",
		name = name,
		time = time,
		location = location,
		type = type
	)).getOrThrow()

	fun getMeetingCount(parent: Resource) = selectRowCount(where = property(Meeting::parent) isEqualTo value("${ parent.resourceType }:${ parent.id.asString() }"))
	fun getMeetings(parent: String, type: ResourceType, order: Order? = null, offset: Int? = null, limit: Int? = null) = select(
		where = property(Meeting::parent) isEqualTo value("${ type }:${ parent }"),
		order = order ?: DEFAULT_ORDER,
		offset = offset,
		limit = limit
	)
}