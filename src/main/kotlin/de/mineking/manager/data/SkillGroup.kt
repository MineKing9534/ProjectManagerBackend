package de.mineking.manager.data

import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.Main
import org.jdbi.v3.core.kotlin.useHandleUnchecked

data class SkillGroup(
	@Transient val main: Main,
	@Column(key = true) override val id: ID = DEFAULT_ID,
	@Column(unique = true) val name: String = ""
) : DataClass<SkillGroup>, Identifiable {
	override fun getTable() = main.skillGroups
}

interface SkillGroupTable : IdentifiableTable<SkillGroup> {
	fun create(name: String): SkillGroup = insert(SkillGroup(main, name = name))
	override fun delete(id: String): Boolean {
		manager.driver.useHandleUnchecked {
			it.createUpdate("update skills set \"group\" = '' where \"group\" = :id")
				.bind("id", id)
				.execute()
		}
		return super.delete(id)
	}
}