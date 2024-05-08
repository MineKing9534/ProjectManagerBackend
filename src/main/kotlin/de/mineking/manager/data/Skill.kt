package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.javautils.ID
import de.mineking.manager.data.type.Identifiable
import de.mineking.manager.main.Main

data class Skill(
	@Transient val main: Main,
	@Column(key = true) override val id: ID? = null,
	@Column(unique = true) val name: String = ""
) : DataClass<Skill>, Identifiable {
	override fun getTable() = main.skills
}
