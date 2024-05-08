package de.mineking.manager.api

import de.mineking.manager.data.User
import de.mineking.manager.main.Main

data class AuthorizationInfo(val main: Main, val user: User)
