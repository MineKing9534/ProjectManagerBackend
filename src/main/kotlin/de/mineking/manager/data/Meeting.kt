package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Where
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main
import java.time.Instant

data class Meeting(
	@Transient override val main: Main,
	@Column(key = true) override val id: ID = DEFAULT_ID,
	@Column override val parent: String = "",
	@Column override val name: String = "",
	@Column val type: MeetingType = MeetingType.MEETING,
	@Column val location: String = "",
	@Column val time: Instant = Instant.now()
) : DataClass<Meeting>, Identifiable, Resource, Comparable<Meeting> {
	override val resourceType = ResourceType.MEETING
	override fun getTable() = main.meetings

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
		val DEFAULT_ORDER = Order.ascendingBy("time")
	}

	fun create(parent: String, name: String, time: Instant, location: String, type: MeetingType): Meeting = insert(Meeting(main, parent = parent, name = name, time = time, location = location, type = type))

	fun getMeetings(team: String, order: Order? = null) = selectMany(Where.equals("parent", team), order ?: DEFAULT_ORDER)
}