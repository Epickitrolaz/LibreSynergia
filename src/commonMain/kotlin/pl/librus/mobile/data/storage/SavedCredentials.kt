package pl.librus.mobile.data.storage

import kotlinx.serialization.Serializable
import pl.librus.mobile.auth.AuthBackendType
import pl.librus.mobile.domain.models.ChildAccount

@Serializable
data class SavedAccount(
    val id: String,
    val backendType: AuthBackendType,
    val identifier: String,
    val passwordObfuscated: String,
    val studentName: String,
    val className: String = "",
    val children: List<ChildAccount> = emptyList(),
    val selectedChildId: String? = null
) {
    val password: String
        get() = SavedCredentials.deobfuscate(passwordObfuscated)

    companion object {
        fun create(
            backendType: AuthBackendType,
            identifier: String,
            rawPassword: String,
            studentName: String,
            className: String = "",
            children: List<ChildAccount> = emptyList(),
            selectedChildId: String? = null
        ): SavedAccount {
            val accountId = "${backendType.name}:${identifier.trim().lowercase()}"
            return SavedAccount(
                id = accountId,
                backendType = backendType,
                identifier = identifier,
                passwordObfuscated = SavedCredentials.obfuscate(rawPassword),
                studentName = studentName,
                className = className,
                children = children,
                selectedChildId = selectedChildId
            )
        }
    }
}

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

        fun obfuscate(plain: String): String {
            return plain.map { (it.code xor 0x5A).toChar() }.joinToString("")
        }

        fun deobfuscate(obf: String): String {
            return obf.map { (it.code xor 0x5A).toChar() }.joinToString("")
        }
    }
}
