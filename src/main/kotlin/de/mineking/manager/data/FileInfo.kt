package de.mineking.manager.data

import de.mineking.javautils.ID
import java.io.File
import java.math.BigInteger
import java.time.Instant
import kotlin.io.path.getLastModifiedTime

enum class FileType {
	DIRECTORY,
	FILE
}

data class FileInfo(
	val name: String,
	val time: Instant,
	val type: FileType,
	override val id: ID = ID.decode(BigInteger.valueOf(time.toEpochMilli()).toByteArray() + 0)
) : Identifiable, Comparable<FileInfo> {
	override fun compareTo(other: FileInfo): Int = Comparator.comparing { f: FileInfo -> f.type.ordinal }
		.thenComparing(FileInfo::name)
		.compare(this, other)
}

fun File.info(): FileInfo {
	return FileInfo(
		name,
		toPath().getLastModifiedTime().toInstant(),
		if (isDirectory) FileType.DIRECTORY else FileType.FILE
	)
}