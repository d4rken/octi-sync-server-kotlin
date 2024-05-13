package eu.darken.octi.kserver.account

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterResponse(
    @Contextual @SerialName("username") val accountID: AccountId,
    @SerialName("password") val password: String
)