package de.mineking.manager.api

import de.mineking.databaseutils.Order
import io.javalin.http.Context

const val ENTRIES_PER_PAGE = 15

data class PaginationResult(
	val page: Int,
	val total: Int,
	val data: Collection<Any>
)

fun paginateResult(ctx: Context, total: Int, getter: (order: Order) -> Collection<Any>, order: Order? = null) {
	with(ctx) {
		val totalPages = ((total - 1) / ENTRIES_PER_PAGE) + 1

		val page = queryParamAsClass("page", Int::class.java)
			.check({ it in 1..totalPages }, "Invalid 'page'")
			.getOrDefault(1)

		json(PaginationResult(
			page,
			totalPages,
			getter((order ?: Order.empty()).offset((page - 1) * ENTRIES_PER_PAGE).limit(ENTRIES_PER_PAGE))
		))
	}
}