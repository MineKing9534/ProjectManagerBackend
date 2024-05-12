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

	fun getAll(order: Order? = DEFAULT_ORDER): List<T> = selectAll(order ?: DEFAULT_ORDER)
	fun getAllIds(order: Order? = DEFAULT_ORDER): List<String> = manager.driver.withHandleUnchecked { it.createQuery("select id from $name ${ (order ?: DEFAULT_ORDER).format() }").mapTo(String::class.java).list() }
	fun getByIds(ids: Collection<Any>, order: Order? = DEFAULT_ORDER): List<T> = selectMany(Where.valueContainsField("id", ids), order ?: DEFAULT_ORDER)
	fun getById(id: String): T? = selectOne(Where.equals("id", id)).orElse(null)
	fun delete(id: String): Boolean = delete(Where.equals("id", id)) > 0
}