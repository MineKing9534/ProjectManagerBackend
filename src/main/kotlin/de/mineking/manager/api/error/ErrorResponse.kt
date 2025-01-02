package de.mineking.manager.api.error

class ErrorResponse(val type: ErrorResponseType, val status: Int = type.status.code): RuntimeException() {
	class Data(e: ErrorResponse) {
		val type: ErrorResponseType = e.type
		val status: Int = e.status
	}

	val data get() = Data(this)
}