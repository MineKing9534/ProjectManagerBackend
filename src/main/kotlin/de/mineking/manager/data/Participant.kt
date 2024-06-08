package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.databaseutils.Table
import de.mineking.databaseutils.Where
import de.mineking.databaseutils.exception.ConflictException
import de.mineking.javautils.ID
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main
import org.jdbi.v3.core.kotlin.withHandleUnchecked

data class Participant(
	@Transient val main: Main,
	@Column(key = true) val user: ID = DEFAULT_ID,
	@Column(key = true) val parent: ID = DEFAULT_ID,
	@Column val parentType: ResourceType = ResourceType.TEAM
) : DataClass<Participant> {
	override fun getTable() = main.participants
}

data class ParticipantInfo(
	val user: User,
	val direct: Boolean
)

interface ParticipantTable : Table<Participant> {
	val main: Main get() = manager.getData<Main>("main")

	@Throws(ErrorResponse::class)
	fun join(user: ID, parent: ID, parentType: ResourceType) {
		if (user == parent) throw ErrorResponse(ErrorResponseType.ALREADY_PARTICIPATING)

		try {
			insert(Participant(main, user, parent, parentType))
		} catch (_: ConflictException) {
			throw ErrorResponse(ErrorResponseType.ALREADY_PARTICIPATING)
		}
	}

	fun leave(user: ID, parent: ID): Boolean = delete(
		Where.allOf(
			Where.equals("user", user),
			Where.equals("parent", parent)
		)
	) > 0

	fun get(user: ID, resource: ID): Participant? = selectOne(
		Where.allOf(
			Where.equals("user", user),
			Where.equals("parent", resource)
		)
	).orElse(null)

	fun getParents(user: ID, parentType: ResourceType): Collection<String> = manager.driver.withHandleUnchecked {
		it.createQuery("select parent from $name where \"user\" = :user and parenttype = :parent")
			.bind("user", user.asString())
			.bind("parent", parentType)
			.mapTo(String::class.java)
			.set()
	}

	fun getDirectParticipants(parent: ID): Collection<String> = manager.driver.withHandleUnchecked {
		it.createQuery("select \"user\" from $name where parent = :parent")
			.bind("parent", parent.asString())
			.mapTo(String::class.java)
			.set()
	}

	fun getParticipantUsers(parent: Resource, recursive: Boolean = true): Collection<String> {
		val result = mutableSetOf<String>()
		getParticipantUsers(result, parent, recursive)

		return result
	}

	private fun getParticipantUsers(result: MutableCollection<String>, parent: Resource, recursive: Boolean) {
		result.addAll(getDirectParticipants(parent.id))
		if (recursive && parent.parent.isNotEmpty()) {
			val temp = parent.resourceType.table(main).getById(parent.parent)
			if (temp != null) getParticipantUsers(result, temp, true)
		}
	}
}