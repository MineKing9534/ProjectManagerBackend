package de.mineking.manager.main

import de.mineking.manager.data.Meeting
import de.mineking.manager.data.Resource
import de.mineking.manager.data.User
import jakarta.mail.Message
import org.simplejavamail.api.email.Recipient
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import java.io.FileNotFoundException


class EmailClient(val main: Main) {
	companion object {
		val strategy: TransportStrategy = TransportStrategy.SMTPS
	}

	private val mailer: Mailer = MailerBuilder
		.withSMTPServer(
			main.credentials["EMAIL_HOST"] ?: throw NullPointerException("EMAIL_HOST required"),
			strategy.defaultServerPort,
			main.credentials["EMAIL_USER"] ?: throw NullPointerException("EMAIL_USER required"),
			main.credentials["EMAIL_PASSWORD"] ?: throw NullPointerException("EMAIL_PASSWORD required")
		)
		.withEmailDefaults(
			EmailBuilder.startingBlank()
				.from("Projektverwaltung", main.credentials["EMAIL_USER"] ?: throw NullPointerException("EMAIL_USER required"))
				.buildEmail()
		)
		.withTransportStrategy(strategy)
		.withSessionTimeout(10 * 1000)
		.async()
		.buildMailer()

	init {
		mailer.testConnection()
	}

	fun sendVerificationEmail(firstName: String, lastName: String, email: String, parent: Resource) {
		val token = main.authenticator.generateVerificationToken(firstName, lastName, email, parent.id.asString(), parent.resourceType)

		mailer.sendMail(
			EmailBuilder.startingBlank()
				.to(email)
				.withSubject(EmailType.VERIFICATION.title)
				.withHTMLText(EmailType.VERIFICATION.format(main, arrayOf(firstName, lastName, token, parent)))
				.buildEmail()
		)
	}

	fun sendPasswordResetEmail(user: User) {
		val token = main.authenticator.generatePasswordResetToken(user.id.asString())

		mailer.sendMail(
			EmailBuilder.startingBlank()
				.to(user.email)
				.withSubject(EmailType.RESET_PASSWORD.title)
				.withHTMLText(EmailType.RESET_PASSWORD.format(main, arrayOf(user.firstName, user.lastName, token)))
				.buildEmail()
		)
	}

	fun sendEmail(type: EmailType, users: Collection<String>, arg: Array<Any> = emptyArray()) {
		val recipients = main.users.getByIds(users).filter { type in it.emailTypes }
		if (recipients.isEmpty()) return

		mailer.sendMail(
			EmailBuilder.startingBlank()
				.bcc(recipients.map { Recipient("${it.firstName} ${it.lastName}", it.email, Message.RecipientType.BCC) })
				.withSubject(type.title)
				.withHTMLText(type.format(main, arg))
				.buildEmail()
		)
	}
}

enum class EmailType(val custom: Boolean = true, val title: String) {
	VERIFICATION(false, title = "Kontoeinrichtung Abschließen") {
		//firstName, lastName, token, parent
		override fun format(main: Main, arg: Array<Any>): String = html
			.replace("%name%", "${arg[0]} ${arg[1]}")
			.replace("%url%", "${main.config.url}/verify?token=${arg[2]}")
			.replace("%target%", (arg[3] as Resource).name)
	},
	RESET_PASSWORD(false, title = "Passwort zurücksetzten") {
		override fun format(main: Main, arg: Array<Any>): String = html
			.replace("%name%", "${arg[0]} ${arg[1]}")
			.replace("%url%", "${main.config.url}/password?token=${arg[2]}")
	},

	MEETING_CREATE(title = "Neues Treffen") {
		//parent, meeting
		override fun format(main: Main, arg: Array<Any>): String = html
			.replace("%parent%", (arg[0] as Resource).name)
			.replace("%name%", (arg[1] as Meeting).name)
			.replace("%url%", "${main.config.url}/@me/meetings/${(arg[1] as Meeting).id.asString()}")
	},
	MEETING_DELETE(title = "Treffen Abgesagt") {
		//parent, meeting
		override fun format(main: Main, arg: Array<Any>): String = html
			.replace("%parent%", (arg[0] as Resource).name)
			.replace("%name%", (arg[1] as Meeting).name)
			.replace("%url%", "${main.config.url}/@me/${(arg[0] as Resource).resourceType.name.lowercase()}s/${(arg[0] as Resource).id.asString()}")
	},
	MEETING_UPDATE(title = "Änderungen für Treffen") {
		//meeting
		override fun format(main: Main, arg: Array<Any>): String = html
			.replace("%name%", (arg[0] as Meeting).name)
			.replace("%url%", "${main.config.url}/@me/meetings/${(arg[0] as Meeting).id.asString()}")
	},

	INFO_UPDATE(title = "Neue Informationen") {
		//resource
		override fun format(main: Main, arg: Array<Any>): String = html
			.replace("%name%", (arg[0] as Resource).name)
			.replace("%url%", "${main.config.url}/@me/${(arg[0] as Resource).resourceType.name.lowercase()}s/${(arg[0] as Resource).id.asString()}")
	};

	val html: String = EmailType::class.java.getResource("/email/${name.lowercase()}.html")?.readText() ?: throw FileNotFoundException("Email $name")

	open fun format(main: Main, arg: Array<Any>): String = html
}