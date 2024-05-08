package de.mineking.manager.data.table

import de.mineking.databaseutils.Where
import de.mineking.manager.data.User

@JvmDefaultWithCompatibility
interface UserTable : IdentifiableTable<User> {
	fun getByEmail(email: String): User? = selectOne(Where.equals("email", email)).orElse(null)

	fun create(firstName: String, lastName: String, email: String, password: String): User = insert(User(main,
		firstName = firstName,
		lastName = lastName,
		email = email,
		password = password
	))
}