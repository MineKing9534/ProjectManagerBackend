package de.mineking.manager.data

import de.mineking.database.*
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.EmailType
import de.mineking.manager.main.Main
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection
import java.util.*

data class User(
	@Transient val main: Main,
	@Key @Column override val id: ID = DEFAULT_ID,
	@Column val admin: Boolean = false,
	@Column val firstName: String = "",
	@Column val lastName: String = "",
	@Unique @Column val email: String = "",
	@Transient @Column val password: String = "",
	@Column val skills: Set<String> = emptySet(),
	@Column val emailTypes: EnumSet<EmailType> = EnumSet.copyOf(EmailType.entries.filter { it.custom }),
	@Json @Column val inputs: MutableMap<String, Any> = hashMapOf(),
) : DataObject<User>, Identifiable {
	@Transient override val table = main.users
	fun credentialsHash(): Int = Objects.hash(email, password)
}

interface UserTable : IdentifiableTable<User> {
	companion object {
		val DEFAULT_ORDER = ascendingBy("lastname").and(ascendingBy("firstname"))
	}

	@Insert
	fun create(
		@Parameter firstName: String,
		@Parameter lastName: String,
		@Parameter email: String,
		@Parameter password: String
	): UpdateResult<User>

	@Select fun getByEmail(@Condition email: String): User?

	override fun delete(id: String): Boolean {
		main.participants.delete(where = property(Participant::user) isEqualTo value(id))
		return super.delete(id)
	}

	fun exportCSV(where: Where? = null): String = structure.manager.driver.withHandleUnchecked {
		var result = ""
		val copy = CopyManager(it.connection as BaseConnection).copyOut("""
			copy (select id as "ID", firstname as "Vorname", lastname as "Nachname", email as "E-Mail" from users 
			${ (where ?: Where.EMPTY).format(structure) } 
			order by firstname, lastname) 
			to stdout delimiter ',' csv header
		""".trimIndent())

		while (true) {
			val line = copy.readFromCopy() ?: break
			result += line.decodeToString()
		}

		result
	}
}