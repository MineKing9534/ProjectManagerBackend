package de.mineking.manager.data.table

import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Table
import de.mineking.databaseutils.Where
import de.mineking.manager.data.type.Identifiable
import de.mineking.manager.main.Main
import org.jdbi.v3.core.kotlin.withHandleUnchecked

interface IdentifiableTable<T : Identifiable> : Table<T> {
	companion object {
		val DEFAULT_ORDER = Order.ascendingBy("id")
	}

	val main: Main get() = manager.getData<Main>("main")

	fun getAll(order: Order? = null): List<T> = selectAll(order ?: Order.empty())
	fun getAllIds(order: Order? = null): List<String> = manager.driver.withHandleUnchecked { it.createQuery("select id from $name ${ (order ?: Order.empty()).format() }").mapTo(String::class.java).list() }
	fun getByIds(ids: Collection<Any>, order: Order? = null): List<T> = selectMany(Where.valueContainsField("id", ids), order ?: Order.empty())
	fun getById(id: String): T? = selectOne(Where.equals("id", id)).orElse(null)
	fun delete(id: String): Boolean = delete(Where.equals("id", id)) > 0
}