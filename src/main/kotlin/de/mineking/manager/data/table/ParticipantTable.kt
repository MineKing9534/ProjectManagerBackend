package de.mineking.manager.data.table

import de.mineking.databaseutils.Table
import de.mineking.databaseutils.Where
import de.mineking.databaseutils.exception.ConflictException
import de.mineking.javautils.ID
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.MemberType
import de.mineking.manager.data.ParentType
import de.mineking.manager.data.Participant
import de.mineking.manager.data.User
import de.mineking.manager.main.Main
import java.util.stream.Stream

interface ParticipantTable : Table<Participant> {
	val main: Main get() = manager.getData<Main>("main")

	@Throws(ErrorResponse::class)
	fun join(member: ID, type: MemberType, parent: ID, parentType: ParentType) {
		if (member == parent) throw ErrorResponse(ErrorResponseType.ALREADY_PARTICIPATING)

		try {
			insert(Participant(main, member, type, parent, parentType))
		} catch (_: ConflictException) {
			throw ErrorResponse(ErrorResponseType.ALREADY_PARTICIPATING)
		}
	}

	fun leave(member: ID, parent: ID): Boolean = delete(Where.allOf(
		Where.equals("member", member),
		Where.equals("parent", parent)
	)) > 0

	fun getParticipants(id: String): List<Participant> = selectMany(Where.equals("parent", id))
	fun getParticipantUsers(id: String, teams: Boolean = false): List<User> = main.users.getByIds(getParticipantUserIds(id, teams))
	fun getParticipantUserIds(id: String, teams: Boolean = false): List<String> = getParticipants(id).stream()
		.flatMap { participant ->
			if (participant.memberType == MemberType.USER) Stream.of(participant.member.asString())
			else if (teams) getParticipantUserIds(participant.member.asString(), true).stream()
			else Stream.empty()
		}
		.toList()

	fun getParents(id: String): List<Participant> = selectMany(Where.equals("member", id))
}