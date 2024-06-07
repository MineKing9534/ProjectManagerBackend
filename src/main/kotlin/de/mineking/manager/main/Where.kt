package de.mineking.manager.main

import de.mineking.databaseutils.ArgumentFactory
import de.mineking.databaseutils.Where
import de.mineking.databaseutils.Where.WhereImpl
import de.mineking.javautils.ID

fun containsAny(name: String, value: Collection<*>): Where {
	if (value.isEmpty()) return Where.FALSE()

	val id = ID.generate().asString()
	return WhereImpl("$name && :$id", mapOf(id to ArgumentFactory.create(name, value) {
		val f = it.columns[name] ?: throw IllegalStateException("Table has no column with name '$name'")
		val type = f.genericType

		val mapper = it.manager.getMapper<Any, Any>(type, f)

		mapper.createArgument(it.manager, type, f, mapper.format(it.manager, type, f, value))
	}))
}