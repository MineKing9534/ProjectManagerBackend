package de.mineking.manager.data

import com.google.gson.reflect.TypeToken
import de.mineking.database.*
import de.mineking.javautils.ID
import de.mineking.manager.main.DEFAULT_ID
import de.mineking.manager.main.JSON
import de.mineking.manager.main.Main
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

data class Input(
	@Transient val main: Main,
	@Key @Column override val id: ID = DEFAULT_ID,
	@Unique @Column val name: String = "",
	@Column val placeholder: String = "",
	@Column val type: InputType = InputType.STRING,
	@Column val config: String = "{}"
) : DataObject<Input>, Identifiable {
	@Transient override val table = main.inputs

	@OptIn(ExperimentalStdlibApi::class)
	inline fun <reified T> getConfig() = JSON.fromJson(config, TypeToken.get(typeOf<T>().javaType)) as T
}

enum class InputType {
	STRING {
		override fun validate(value: Any, input: Input): Boolean {
			val config = input.getConfig<StringConfig>()

			if (value !is String) return false
			if (config.minLength != null && value.length < config.minLength) return false
			if (config.maxLength != null && value.length > config.maxLength) return false

			return true
		}
	},
	INTEGER {
		override fun validate(value: Any, input: Input): Boolean {
			val config = input.getConfig<IntegerConfig>()

			if (value !is Number) return false
			if (config.minValue != null && value.toInt() < config.minValue) return false
			if (config.maxValue != null && value.toInt() > config.maxValue) return false

			return true
		}
	},
	SELECT {
		override fun validate(value: Any, input: Input): Boolean {
			val allowed = input.getConfig<List<String>>()

			if (value !is String) return false
			if (value !in allowed) return false

			return true
		}
	};

	abstract fun validate(value: Any, input: Input): Boolean

	data class StringConfig(val minLength: Int?, val maxLength: Int?)
	data class IntegerConfig(val minValue: Int?, val maxValue: Int?)
}

interface InputTable : IdentifiableTable<Input> {
	@Insert
	fun create(
		@Parameter name: String,
		@Parameter placeholder: String,
		@Parameter type: InputType,
		@Parameter config: String
	): UpdateResult<Input>
}
