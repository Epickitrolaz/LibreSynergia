package pl.librus.mobile.data.storage

import kotlinx.serialization.Serializable
import pl.librus.mobile.auth.AuthBackendType

@Serializable
data class SavedCredentials(
    val backendType: AuthBackendType,
    val identifier: String,
    val passwordObfuscated: String,
    val selectedChildId: String? = null
) {
    val password: String
        get() = deobfuscate(passwordObfuscated)

    companion object {
        fun create(
            backendType: AuthBackendType,
            identifier: String,
            rawPassword: String,
            selectedChildId: String? = null
        ): SavedCredentials {
            return SavedCredentials(
                backendType = backendType,
                identifier = identifier,
                passwordObfuscated = obfuscate(rawPassword),
                selectedChildId = selectedChildId
            )
        }

        private fun obfuscate(plain: String): String {
            return plain.map { (it.code xor 0x5A).toChar() }.joinToString("")
        }

        private fun deobfuscate(obf: String): String {
            return obf.map { (it.code xor 0x5A).toChar() }.joinToString("")
        }
    }
}
