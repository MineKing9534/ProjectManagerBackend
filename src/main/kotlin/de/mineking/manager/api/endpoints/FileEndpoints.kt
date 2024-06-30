package de.mineking.manager.api.endpoints

import de.mineking.manager.api.checkAuthorization
import de.mineking.manager.api.error.ErrorResponse
import de.mineking.manager.api.error.ErrorResponseType
import de.mineking.manager.api.main
import de.mineking.manager.api.paginateResult
import de.mineking.manager.data.Resource
import de.mineking.manager.data.ResourceType
import de.mineking.manager.data.info
import de.mineking.manager.main.EmailType
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.HandlerType
import io.javalin.http.bodyAsClass
import jakarta.activation.FileDataSource
import org.apache.commons.io.FileUtils
import org.apache.tika.mime.MimeTypes
import org.simplejavamail.api.email.AttachmentResource
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.io.path.getAttribute
import kotlin.io.path.setAttribute


fun FileEndpoints() {
	before {
		with(it) {
			val auth = checkAuthorization()

			val id = pathParam("id")
			val type = attribute<ResourceType>("type")!!
			val resource = type.table(main).getById(id) ?: throw ErrorResponse(type.error)

			if (!auth.user.admin && !resource.canBeAccessed(auth.user.id.asString())) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

			attribute("resource", resource)

			val folder = File("files/${resource.id.asString()}")
			folder.mkdirs()

			attribute("folder", folder)
		}
	}

	get {
		with(it) {
			val folder = attribute<File>("folder")
			folder?.listFiles()?.let { paginateResult(it.map { it.info() }) }
		}
	}

	path("<name>") {
		before {
			with(it) {
				val folder = attribute<File>("folder")!!
				val file = File(folder, pathParam("name"))

				if (file == folder) throw ErrorResponse(ErrorResponseType.INVALID_REQUEST)

				if (!file.exists() && method() != HandlerType.PUT) throw ErrorResponse(ErrorResponseType.FILE_NOT_FOUND)
				if (!file.canonicalPath.startsWith(folder.canonicalPath)) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

				attribute("file", file)
			}
		}

		head {}

		get {
			with(it) {
				val file = attribute<File>("file")
				val list = file?.listFiles()

				if (list != null) paginateResult(list.map { it.info() })
				else throw ErrorResponse(ErrorResponseType.INVALID_FILE_TYPE)
			}
		}

		post {
			with(it) {
				val file = attribute<File>("file")!!
				if (file.isDirectory) throw ErrorResponse(ErrorResponseType.INVALID_FILE_TYPE)

				result(FileInputStream(file))
				header("Content-Disposition", "inline; filename=${file.name}")
				header("Content-Type", (file.toPath().getAttribute("user:mime-type") as ByteArray).toString(StandardCharsets.UTF_8))
			}
		}

		put {
			with(it) {
				checkAuthorization(admin = true)

				val resource = attribute<Resource>("resource")!!

				val file = attribute<File>("file")!!
				val upload = uploadedFile("file")

				if (upload != null && (!file.exists() || file.isFile())) {
					if (!file.exists()) {
						file.getParentFile().mkdirs()
						file.createNewFile()
					}

					upload.content().use { input ->
						file.outputStream().use { output ->
							input.copyTo(output)
						}
					}

					file.writeBytes(upload.content().readAllBytes())
					file.toPath().setAttribute("user:mime-type", ByteBuffer.wrap(upload.contentType()?.toByteArray()))

					if (file.name == "Information") {
						main.email.sendEmail(
							EmailType.INFO_UPDATE, main.participants.getParticipantUsers(resource),
							arrayOf(resource),
							listOf(AttachmentResource("Information${ MimeTypes.getDefaultMimeTypes().forName(upload.contentType()).extension }", FileDataSource(file)))
						)
					}
				} else if (upload == null && !file.exists()) {
					if (file.name == "Information") throw ErrorResponse(ErrorResponseType.INVALID_FILE_TYPE)
					file.mkdirs()
				}
			}
		}

		patch {
			with(it) {
				data class Request(val name: String)

				val request = bodyAsClass<Request>()

				val folder = attribute<File>("folder")!!
				val file = attribute<File>("file")!!

				val newFile = File(file.parentFile, request.name)

				if (!newFile.canonicalPath.startsWith(folder.canonicalPath)) throw ErrorResponse(ErrorResponseType.MISSING_ACCESS)

				if (!file.renameTo(newFile)) throw ErrorResponse(ErrorResponseType.UNKNOWN)
			}
		}

		delete {
			with(it) {
				checkAuthorization(admin = true)

				val file = attribute<File>("file")!!

				if (!FileUtils.deleteQuietly(file)) throw ErrorResponse(ErrorResponseType.FILE_NOT_FOUND)
			}
		}
	}
}