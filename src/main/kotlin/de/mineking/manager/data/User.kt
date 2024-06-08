package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Where
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
	@Column(key = true) override val id: ID = DEFAULT_ID,
	@Column val admin: Boolean = false,
	@Column val firstName: String = "",
	@Column val lastName: String = "",
	@Column(unique = true) val email: String = "",
	@Transient @Column val password: String = "",
	@Column val skills: List<String> = emptyList(),
	@Column val emailTypes: EnumSet<EmailType> = EnumSet.copyOf(EmailType.entries.filter { it.custom })
) : DataClass<User>, Identifiable {
	override fun getTable() = main.users
	override fun hashCode(): Int = Objects.hash(email, password)
}

@JvmDefaultWithCompatibility
interface UserTable : IdentifiableTable<User> {
	companion object {
		val DEFAULT_ORDER = Order.ascendingBy("lastname").andAscendingBy("firstname")
	}

	fun getByEmail(email: String): User? = selectOne(Where.equals("email", email)).orElse(null)

	fun create(firstName: String, lastName: String, email: String, password: String): User = insert(
		User(
			main,
			firstName = firstName,
			lastName = lastName,
			email = email,
			password = password
		)
	)

	override fun delete(id: String): Boolean {
		main.participants.delete(Where.equals("user", id))
		return super.delete(id)
	}

	fun exportCSV(where: Where? = null): String = manager.driver.withHandleUnchecked {
		var result = ""
		val copy = CopyManager(it.connection as BaseConnection).copyOut("""copy (select id as "ID", firstname as "Vorname", lastname as "Nachname", email as "E-Mail" from users ${(where ?: Where.empty()).format()} order by firstname, lastname) to stdout delimiter ',' csv header""".trimMargin())

		while (true) {
			val line = copy.readFromCopy() ?: break
			result += line.decodeToString()
		}

		result
	}
}