package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.javautils.ID
import de.mineking.manager.data.type.Identifiable
import de.mineking.manager.main.EmailType
import de.mineking.manager.main.Main
import java.util.*

data class User(
	@Transient val main: Main,
	@Column(key = true) override val id: ID? = null,
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
