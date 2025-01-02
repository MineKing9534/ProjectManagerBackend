package de.mineking.manager.main

import de.mineking.database.Node
import de.mineking.database.Where
import de.mineking.database.typeMapper
import de.mineking.database.vendors.postgres.PostgresMappers
import de.mineking.javautils.ID

infix fun Node<*>.containsAny(node: Node<*>) = Where(this + " && " + node)
fun idTypeMapper() = typeMapper<ID?, String?>(PostgresMappers.STRING,
	{ it?.let { ID.decode(it) } },
	{ if (it != null && it.asNumber().toInt() != 0) it.asString() else ID.generate().asString() }
)