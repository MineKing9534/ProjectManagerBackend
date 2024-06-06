package de.mineking.manager.main

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.password4j.Password
import de.mineking.databaseutils.DatabaseManager
import de.mineking.javautils.ID
import de.mineking.manager.api.Authenticator
import de.mineking.manager.api.Server
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.data.*
import io.github.cdimascio.dotenv.Dotenv
import java.math.BigInteger
import java.time.DateTimeException
import java.time.Instant
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

fun hashPassword(password: String) = Password.hash(password)
	.addRandomSalt()
	.withArgon2().result

fun verifyPassword(hash: String, password: String) = Password.check(password, hash).withArgon2()

class Main(val config: Config, val credentials: Dotenv) {
	private val server = Server(this)
	private val database = DatabaseManager(
		"jdbc:postgresql://" + credentials["DATABASE_HOST"],
		credentials["DATABASE_USER"] ?: throw NullPointerException("DATABASE_USER required"),
		credentials["DATABASE_PASSWORD"] ?: throw NullPointerException("DATABASE_PASSWORD required")
	)

	val authenticator = Authenticator(this)
	val email = EmailClient(this)

	val meetings: MeetingTable
	val participants: ParticipantTable
	val skills: SkillTable
	val teams: TeamTable
	val users: UserTable

	init {
		this.database.putData("main", this)

		this.meetings = database.getTable(MeetingTable::class.java, Meeting::class.java, { Meeting(this) }, "meetings").createTable() as MeetingTable
		this.participants = database.getTable(ParticipantTable::class.java, Participant::class.java, { Participant(this) }, "participants").createTable() as ParticipantTable
		this.skills = database.getTable(SkillTable::class.java, Skill::class.java, { Skill(this) }, "skills").createTable() as SkillTable
		this.teams = database.getTable(TeamTable::class.java, Team::class.java, { Team(this) }, "teams").createTable() as TeamTable
		this.users = database.getTable(UserTable::class.java, User::class.java, { User(this) }, "users").createTable() as UserTable
	}

	fun start() = server.start()
}

fun main() {
	val config = Config.readFromFile("config")
	val credentials = Dotenv.configure().filename("credentials").load()

	val main = Main(config, credentials)

	main.start()
}

val DEFAULT_ID = ID.decode(BigInteger.ZERO)
val JSON: Gson = GsonBuilder()
	.registerTypeAdapter(ID::class.java, object : TypeAdapter<ID>() {
		override fun write(writer: JsonWriter?, value: ID?) {
			writer?.value(value?.asString())
		}

		override fun read(reader: JsonReader?): ID? {
			val id = reader?.nextString()
			return if (id == null) null else ID.decode(id)
		}
	})
	.registerTypeAdapter(Instant::class.java, object : TypeAdapter<Instant>() {
		override fun write(writer: JsonWriter?, value: Instant?) {
			writer?.value(value?.toString())
		}

		override fun read(reader: JsonReader?): Instant? {
			try {
				val value = reader?.nextString()
				return if (value == null) null else Instant.parse(value)
			} catch (e: DateTimeException) {
				throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
			}
		}
	})
	.registerTypeAdapterFactory(object : TypeAdapterFactory {
		override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
			val delegate = gson.getDelegateAdapter(this, type)

			if (type.rawType.declaredAnnotations.none { it.annotationClass.qualifiedName == "kotlin.Metadata" }) return null

			return object : TypeAdapter<T>() {
				override fun write(out: JsonWriter, value: T?) = delegate.write(out, value)
				override fun read(input: JsonReader): T? {
					val value: T? = delegate.read(input)

					if (value != null) {
						val kotlinClass: KClass<Any> = Reflection.createKotlinClass(type.rawType)

						kotlinClass.memberProperties.forEach { field ->
							if (!field.returnType.isMarkedNullable && field.get(value) == null) {
								throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)
							}
						}

					}

					return value
				}
			}
		}
	})
	.create()