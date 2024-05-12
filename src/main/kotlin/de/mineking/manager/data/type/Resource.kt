package de.mineking.manager.data.type

import de.mineking.manager.data.ParentType

interface Resource : Identifiable {
	val name: String
	val resourceType: ParentType
}