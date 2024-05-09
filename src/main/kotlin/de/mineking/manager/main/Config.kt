package de.mineking.manager.main

import java.io.File

data class Config(
	val port: Int,
	val url: String,
	val info: Info
) {
	companion object {
		fun readFromFile(name: String): Config = JSON.fromJson(File(name).readText(), Config::class.java) ?: throw NullPointerException("Config could not be read")
	}
}

data class Info(val provider: String, val author: String, val title: String)