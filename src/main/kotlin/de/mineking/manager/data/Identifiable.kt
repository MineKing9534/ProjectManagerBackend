package de.mineking.manager.data

import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Table
import de.mineking.databaseutils.Where
import de.mineking.javautils.ID
import de.mineking.manager.main.Main
import org.jdbi.v3.core.kotlin.withHandleUnchecked

interface Identifiable {
	val id: ID
}

interface IdentifiableTable<T : Identifiable> : Table<T> {
	companion object {
		val DEFAULT_ORDER = Order.ascendingBy("id")
	}

	val main: Main get() = manager.getData<Main>("main")

	fun getAll(order: Order? = null): List<T> = selectAll(order ?: DEFAULT_ORDER)
	fun getAllIds(order: Order? = null): List<String> = manager.driver.withHandleUnchecked {
		it.createQuery("select id from $name ${(order ?: DEFAULT_ORDER).format()}")
			.mapTo(String::class.java)
			.list()
	}

	fun getByIds(ids: Collection<Any>, order: Order? = null, where: Where? = null): List<T> = selectMany(Where.valueContainsField("id", ids).and(where ?: Where.empty()), order ?: DEFAULT_ORDER)
	fun getById(id: String): T? = selectOne(Where.equals("id", id)).orElse(null)
	fun delete(id: String): Boolean = delete(Where.equals("id", id)) > 0
}