package de.mineking.manager.data

import de.mineking.database.*
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main

data class Skill(
	@Transient val main: Main,
	@Key @Column override val id: ID = DEFAULT_ID,
	@Unique @Column val name: String = "",
	@Column val group: String = ""
) : DataObject<Skill>, Identifiable {
	@Transient override val table = main.skills
}

interface SkillTable : IdentifiableTable<Skill> {
	@Insert fun create(@Parameter name: String, @Parameter group: String): UpdateResult<Skill>
	override fun delete(id: String): Boolean {
		main.users.update(property(User::skills) to "array_remove"(property(User::skills), value(id)))
		return super.delete(id)
	}
}
