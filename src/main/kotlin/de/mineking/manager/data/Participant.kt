package de.mineking.manager.data

import de.mineking.database.*
import de.mineking.javautils.ID
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main
import org.jdbi.v3.core.kotlin.withHandleUnchecked

data class Participant(
	@Transient val main: Main,
	@Key @Column val user: ID = DEFAULT_ID,
	@Key @Column val parent: ID = DEFAULT_ID,
	@Column val parentType: ResourceType = ResourceType.TEAM
) : DataObject<Participant> {
	@Transient override val table = main.participants
}

data class ParticipantInfo(
	val user: User,
	val direct: Boolean
)

interface ParticipantTable : Table<Participant> {
	val main: Main get() = structure.manager.data["main"] as Main

	@Insert
	fun join(@Parameter user: ID, @Parameter parent: ID, @Parameter parentType: ResourceType) {
		if (user == parent) throw ErrorResponse(ErrorResponseType.ALREADY_PARTICIPATING)

		if (execute<UpdateResult<Participant>>().uniqueViolation) throw ErrorResponse(ErrorResponseType.ALREADY_PARTICIPATING)
	}

	@Delete fun leave(@Condition user: ID, @Condition parent: ID): Boolean
	@Select fun get(@Condition user: ID, @Condition parent: ID): Participant?

	fun getParents(user: ID, parentType: ResourceType) = getParents(user, property(Participant::parentType) isEqualTo value(parentType))
	fun getParents(user: ID, where: Where): Set<String> = selectValue(property<String>(Participant::parent), where = property(Participant::user) isEqualTo value(user) and where).list().toSet()

	@SelectValue("user") fun getDirectParticipants(@Condition parent: ID): Set<String>

	fun getParticipantUsers(parent: Resource, recursive: Boolean = true): Set<String> {
		val result = mutableSetOf<String>()
		getParticipantUsers(result, parent, recursive)

		return result
	}

	private fun getParticipantUsers(result: MutableCollection<String>, parent: Resource, recursive: Boolean) {
		result.addAll(getDirectParticipants(parent.id))
		if (recursive && parent.parent.isNotEmpty()) {
			val temp = parent.getParent()
			if (temp != null) getParticipantUsers(result, temp, true)
		}
	}
}