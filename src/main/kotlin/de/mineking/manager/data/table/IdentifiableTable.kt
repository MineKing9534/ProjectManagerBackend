package de.mineking.manager.data.table

import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Table
import de.mineking.databaseutils.Where
import de.mineking.manager.data.type.Identifiable
import de.mineking.manager.main.Main
import org.jdbi.v3.core.kotlin.withHandleUnchecked

interface IdentifiableTable<T : Identifiable> : Table<T> {
	val main: Main get() = manager.getData<Main>("main")

	fun getAll(sort: String? = "id"): List<T> = selectAll(Order.ascendingBy(sort ?: "id"))
	fun getAllIds(sort: String? = "id"): List<String> = manager.driver.withHandleUnchecked { it.createQuery("select id from $name order by ${ sort ?: "id" } asc").mapTo(String::class.java).list() }
	fun getByIds(ids: Collection<Any>): List<T> = selectMany(Where.valueContainsField("id", ids))
	fun getById(id: String): T? = selectOne(Where.equals("id", id)).orElse(null)
	fun delete(id: String): Boolean = delete(Where.equals("id", id)) > 0
}