package de.mineking.manager.data

import com.google.gson.reflect.TypeToken
import de.mineking.databaseutils.Column
import de.mineking.databaseutils.DataClass
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.JSON
import de.mineking.manager.main.Main

data class Input(
	@Transient val main: Main,
	@Column(key = true) override val id: ID = DEFAULT_ID,
	@Column(unique = true) val name: String = "",
	@Column val placeholder: String = "",
	@Column val type: InputType = InputType.STRING,
	@Column val config: String = "{}"
) : DataClass<Input>, Identifiable {
	override fun getTable() = main.inputs

	inline fun <reified T> getConfig() = JSON.fromJson(config, T::class.java)
}

enum class InputType {
	STRING {
		override fun validate(value: Any, config: String): Boolean {
			val c = JSON.fromJson(config, StringConfig::class.java)

			if (value !is String) return false
			if (c.minLength != null && value.length < c.minLength) return false
			if (c.maxLength != null && value.length > c.maxLength) return false

			return true
		}
	},
	INTEGER {
		override fun validate(value: Any, config: String): Boolean {
			val c = JSON.fromJson(config, IntegerConfig::class.java)

			if (value !is Number) return false
			if (c.minValue != null && value.toInt() < c.minValue) return false
			if (c.maxValue != null && value.toInt() > c.maxValue) return false

			return true
		}
	},
	SELECT {
		@Suppress("UNCHECKED_CAST")
		override fun validate(value: Any, config: String): Boolean {
			val allowed = JSON.fromJson(config, TypeToken.getArray(String::class.java)) as Array<String>

			if (value !is String) return false
			if (value !in allowed) return false

			return true
		}
	};

	abstract fun validate(value: Any, config: String): Boolean

	data class StringConfig(val minLength: Int?, val maxLength: Int?)
	data class IntegerConfig(val minValue: Int?, val maxValue: Int?)
}

interface InputTable : IdentifiableTable<Input> {
	fun create(name: String, placeholder: String, type: InputType, config: Any) = insert(Input(main,
		name = name,
		placeholder = placeholder,
		type = type,
		config = JSON.toJson(config)
	))
}
