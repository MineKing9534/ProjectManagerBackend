package de.mineking.manager.data

import de.mineking.database.*
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main

data class SkillGroup(
	@Transient val main: Main,
	@Key @Column override val id: ID = DEFAULT_ID,
	@Unique @Column val name: String = ""
) : DataObject<SkillGroup>, Identifiable {
	@Transient override val table = main.skillGroups
}

interface SkillGroupTable : IdentifiableTable<SkillGroup> {
	@Insert fun create(@Parameter name: String): UpdateResult<SkillGroup>
	override fun delete(id: String): Boolean {
		main.skills.update(property(Skill::group) to value(""), where = property(Skill::group) isEqualTo value(id))
		return super.delete(id)
	}
}