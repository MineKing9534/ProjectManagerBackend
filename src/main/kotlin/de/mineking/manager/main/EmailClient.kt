package de.mineking.manager.main

class EmailClient {
	companion object {
		val EMAIL_PATTERN: Regex = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$".toRegex()
		fun isValidEmail(email: String): Boolean = email.matches(EMAIL_PATTERN)
	}

}