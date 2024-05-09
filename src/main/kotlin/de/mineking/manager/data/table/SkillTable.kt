package de.mineking.manager.data.table

import de.mineking.manager.data.Skill
import org.jdbi.v3.core.kotlin.useHandleUnchecked

interface SkillTable : IdentifiableTable<Skill> {
	fun create(name: String): Skill = insert(Skill(main, name = name))
	override fun delete(id: String): Boolean {
		manager.driver.useHandleUnchecked {
			it.createUpdate("update users set skills = array_remove(skills, :id)")
				.bind("id", id)
				.execute()
		}
		return super.delete(id)
	}
}
