package de.mineking.manager.data.table

import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Where
import de.mineking.manager.data.User
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection

@JvmDefaultWithCompatibility
interface UserTable : IdentifiableTable<User> {
	companion object {
		val DEFAULT_ORDER = Order.ascendingBy("firstname").andAscendingBy("lastname")
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

	fun exportCSV(): String = manager.driver.withHandleUnchecked {
		var result = ""
		val copy = CopyManager(it.connection as BaseConnection).copyOut("""copy (select id as "ID", firstname as "Vorname", lastname as "Nachname", email as "E-Mail" from users order by firstname, lastname) to stdout delimiter ',' csv header""".trimMargin())

		while (true) {
			val line = copy.readFromCopy() ?: break
			result += line.decodeToString()
		}

		result
	}
}