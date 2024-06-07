package de.mineking.manager.data

import java.io.File
import java.time.Instant
import kotlin.io.path.getLastModifiedTime

enum class FileType {
	DIRECTORY,
	FILE
}

data class FileInfo(
	val id: Int,
	val name: String,
	val time: Instant,
	val type: FileType
) : Comparable<FileInfo> {
	override fun compareTo(other: FileInfo): Int = Comparator.comparing { f: FileInfo -> f.type.ordinal }
		.thenComparing(FileInfo::name)
		.compare(this, other)
}

fun File.info(): FileInfo {
	return FileInfo(
		hashCode(),
		name,
		toPath().getLastModifiedTime().toInstant(),
		if(isDirectory) FileType.DIRECTORY else FileType.FILE
	)
}