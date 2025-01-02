package de.mineking.manager.data

import de.mineking.database.*
import de.mineking.database.vendors.postgres.contains
import de.mineking.javautils.ID
import de.mineking.manager.main.Main

interface Identifiable {
	val id: ID
}

interface IdentifiableTable<T : Identifiable> : Table<T> {
	companion object {
		val DEFAULT_ORDER = ascendingBy("id")
	}

	val main: Main get() = data("main")

	fun getAll(order: Order? = null, offset: Int? = null, limit: Int? = null): List<T> = select(order = order ?: DEFAULT_ORDER, offset = offset, limit = limit).list()
	fun getAllIds(order: Order? = null): Set<String> = selectValue(property<String>(Identifiable::id), order = order).list().toSet()

	fun getByIds(ids: Set<String>, where: Where? = null, order: Order? = null, offset: Int? = null, limit: Int? = null): List<T> = select(
		where = (value(ids) contains property(Identifiable::id)) and (where ?: Where.EMPTY),
		order = order ?: DEFAULT_ORDER,
		offset = offset,
		limit = limit
	).list()

	@Select fun getById(@Condition id: String): T?
	@Delete fun delete(@Condition id: String): Boolean = execute() //Default implementation is required because of compiler errors in inheritors otherwise
}