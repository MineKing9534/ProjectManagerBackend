package de.mineking.manager.api

import de.mineking.database.Order
import de.mineking.manager.data.Identifiable
import io.javalin.http.Context

const val ENTRIES_PER_PAGE = 15

data class PaginationResult(
	val page: Int,
	val totalPages: Int,
	val totalEntries: Int,
	val data: Collection<Any>
)

private fun Context.paginateResult(total: Int, elements: (page: Int, entriesPerPage: Int) -> Collection<Any>) {
	val entriesPerPage = queryParamAsClass("entriesPerPage", Int::class.java).getOrDefault(ENTRIES_PER_PAGE)

	val totalPages = ((total - 1) / entriesPerPage) + 1

	val page = queryParamAsClass("page", Int::class.java).allowNullable()
		.check({ it == null || it in 1..totalPages }, "Invalid 'page'")
		.get()

	if (page == null) json(elements(1, Integer.MAX_VALUE))
	else json(
		PaginationResult(
			page,
			totalPages,
			total,
			elements(page, entriesPerPage)
		)
	)
}

fun Context.paginateResult(total: Int, getter: (order: Order, offset: Int, limit: Int) -> Collection<Any>, order: Order? = null) = paginateResult(total) { page, entriesPerPage ->
	getter(order ?: Order { "" }, (page - 1) * entriesPerPage, entriesPerPage)
}

fun <T> Context.paginateResult(elements: Collection<T>) where T : Comparable<T>, T : Identifiable = paginateResult(elements.size) { page, entriesPerPage ->
	elements.distinctBy { it.id.asString() }
		.sorted()
		.drop((page - 1) * entriesPerPage)
		.take(entriesPerPage)
		.toList()
}