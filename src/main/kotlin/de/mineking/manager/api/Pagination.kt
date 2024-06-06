package de.mineking.manager.api

import de.mineking.databaseutils.Order
import io.javalin.http.Context

const val ENTRIES_PER_PAGE = 15

data class PaginationResult(
	val page: Int,
	val total: Int,
	val data: Collection<Any>
)

private fun Context.paginateResult(total: Int, elements: (Int, Int) -> Collection<Any>) {
	val entriesPerPage = queryParamAsClass("entriesPerPage", Int::class.java).getOrDefault(ENTRIES_PER_PAGE)

	val totalPages = ((total - 1) / entriesPerPage) + 1

	val page = queryParamAsClass("page", Int::class.java).allowNullable()
		.check({ it == null || it in 1..totalPages }, "Invalid 'page'")
		.get()

	if(page == null) json(elements(1, Integer.MAX_VALUE))
	else json(PaginationResult(
		page,
		totalPages,
		elements.invoke(page, entriesPerPage)
	))
}

fun Context.paginateResult(total: Int, getter: (order: Order) -> Collection<Any>, order: Order? = null) = paginateResult(total) { page, entriesPerPage ->
	getter((order ?: Order.empty()).offset((page - 1) * entriesPerPage).limit(entriesPerPage))
}

fun Context.paginateResult(elements: Collection<Comparable<*>>) = paginateResult(elements.size) { page, entriesPerPage ->
	elements.stream().unordered()
		.skip((page - 1) * entriesPerPage.toLong())
		.limit(entriesPerPage.toLong())
		.sorted()
		.toList()
}