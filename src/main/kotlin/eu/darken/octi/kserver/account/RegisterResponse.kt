package eu.darken.octi.kserver.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterResponse(
    @SerialName("username") val accountID: String,
    @SerialName("password") val password: String
)