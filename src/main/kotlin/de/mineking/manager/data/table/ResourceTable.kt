package de.mineking.manager.data.table

import de.mineking.databaseutils.Order
import de.mineking.databaseutils.Where
import de.mineking.manager.data.type.Resource

interface ResourceTable<T : Resource> : IdentifiableTable<T> {
	companion object {
		val DEFAULT_ORDER = Order.ascendingBy("name")
	}

	override fun delete(id: String): Boolean {
		main.participants.delete(Where.equals("parent", id))
		return super.delete(id)
	}
}