package pl.librus.mobile.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.librus.mobile.data.MockDataProvider
import pl.librus.mobile.data.storage.CachedAppState
import pl.librus.mobile.data.storage.PersistentStorage
import pl.librus.mobile.data.storage.SavedCredentials
import pl.librus.mobile.domain.models.ChildAccount
import pl.librus.mobile.domain.models.StudentProfile

enum class AuthBackendType {
    PORTAL_RODZINA,
    GATEWAY_API_20,
    DEMO_MOCK
}

data class UserSession(
    val backendType: AuthBackendType,
    val identifier: String, // email or numeric login
    val children: List<ChildAccount>,
    val activeChild: ChildAccount,
    val profile: StudentProfile
)

sealed class AuthState {
    data object Unauthenticated : AuthState()
    data class Authenticating(val message: String = "Logowanie...") : AuthState()
    data class Authenticated(val session: UserSession) : AuthState()
    data class Error(val errorMessage: String) : AuthState()
}

class AuthManager(private val storage: PersistentStorage) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val _state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _isBackgroundSyncing = MutableStateFlow(false)
    val isBackgroundSyncing: StateFlow<Boolean> = _isBackgroundSyncing.asStateFlow()

    private val _backgroundSyncMessage = MutableStateFlow<String?>(null)
    val backgroundSyncMessage: StateFlow<String?> = _backgroundSyncMessage.asStateFlow()

    init {
        restoreSessionFromStorage()
    }

    private fun restoreSessionFromStorage() {
        val credsJson = storage.getString(KEY_SAVED_CREDENTIALS) ?: return
        val creds = try {
            json.decodeFromString<SavedCredentials>(credsJson)
        } catch (_: Exception) {
            return
        }

        if (creds.backendType == AuthBackendType.DEMO_MOCK) {
            loginDemo()
            return
        }

        val cachedJson = storage.getString(KEY_CACHED_STATE)
        val cached = cachedJson?.let {
            try {
                json.decodeFromString<CachedAppState>(it)
            } catch (_: Exception) {
                null
            }
        }

        val children = cached?.children?.takeIf { it.isNotEmpty() } ?: listOf(
            ChildAccount(
                id = creds.selectedChildId ?: creds.identifier,
                accountIdentifier = creds.identifier,
                studentName = cached?.profile?.fullName ?: creds.identifier,
                login = creds.identifier,
                accessToken = ""
            )
        )
        val active = children.find { it.id == creds.selectedChildId } ?: children.first()
        val profile = cached?.profile ?: StudentProfile(
            id = active.id,
            firstName = active.studentName.split(" ").firstOrNull() ?: active.studentName,
            lastName = active.studentName.split(" ").drop(1).joinToString(" "),
            fullName = active.studentName,
            accountEmail = creds.identifier
        )

        _state.value = AuthState.Authenticated(
            UserSession(
                backendType = creds.backendType,
                identifier = creds.identifier,
                children = children,
                activeChild = active,
                profile = profile
            )
        )
    }

    suspend fun performBackgroundSync(
        portalClient: PortalAuthClient,
        gatewayClient: GatewayAuthClient,
        onSessionRefreshed: suspend (UserSession) -> Unit
    ) {
        val credsJson = storage.getString(KEY_SAVED_CREDENTIALS) ?: return
        val creds = try {
            json.decodeFromString<SavedCredentials>(credsJson)
        } catch (_: Exception) {
            return
        }

        if (creds.backendType == AuthBackendType.DEMO_MOCK) return

        _isBackgroundSyncing.value = true
        _backgroundSyncMessage.value = "Logowanie i aktualizacja danych..."

        try {
            val updatedSession = when (creds.backendType) {
                AuthBackendType.PORTAL_RODZINA -> {
                    val children = portalClient.login(creds.identifier, creds.password)
                    val active = children.find { it.id == creds.selectedChildId } ?: children.first()
                    val profile = StudentProfile(
                        id = active.id,
                        firstName = active.studentName.split(" ").firstOrNull() ?: active.studentName,
                        lastName = active.studentName.split(" ").drop(1).joinToString(" "),
                        fullName = active.studentName,
                        accountEmail = creds.identifier
                    )
                    UserSession(
                        backendType = AuthBackendType.PORTAL_RODZINA,
                        identifier = creds.identifier,
                        children = children,
                        activeChild = active,
                        profile = profile
                    )
                }
                AuthBackendType.GATEWAY_API_20 -> {
                    val gatewaySession = gatewayClient.login(creds.identifier, creds.password)
                    val active = gatewaySession.childAccount
                    val profile = StudentProfile(
                        id = active.id,
                        firstName = active.studentName,
                        lastName = "",
                        fullName = active.studentName,
                        className = gatewaySession.className
                    )
                    UserSession(
                        backendType = AuthBackendType.GATEWAY_API_20,
                        identifier = creds.identifier,
                        children = listOf(active),
                        activeChild = active,
                        profile = profile
                    )
                }
                AuthBackendType.DEMO_MOCK -> null
            }

            if (updatedSession != null) {
                _state.value = AuthState.Authenticated(updatedSession)
                onSessionRefreshed(updatedSession)
            }
        } catch (e: Exception) {
            // Keep existing cached state if background re-login fails (e.g. offline)
        } finally {
            _isBackgroundSyncing.value = false
            _backgroundSyncMessage.value = null
        }
    }

    fun loginDemo() {
        val children = MockDataProvider.mockStudents
        val activeChild = children.first()
        val profile = MockDataProvider.getStudentProfile(activeChild)

        val session = UserSession(
            backendType = AuthBackendType.DEMO_MOCK,
            identifier = "demo@librus.pl",
            children = children,
            activeChild = activeChild,
            profile = profile
        )
        _state.value = AuthState.Authenticated(session)

        val creds = SavedCredentials.create(
            backendType = AuthBackendType.DEMO_MOCK,
            identifier = "demo@librus.pl",
            rawPassword = "demo",
            selectedChildId = activeChild.id
        )
        storage.putString(KEY_SAVED_CREDENTIALS, json.encodeToString(creds))
    }

    fun switchChild(childId: String) {
        val current = _state.value as? AuthState.Authenticated ?: return
        val newChild = current.session.children.find { it.id == childId } ?: return

        val newProfile = if (current.session.backendType == AuthBackendType.DEMO_MOCK) {
            MockDataProvider.getStudentProfile(newChild)
        } else {
            current.session.profile.copy(
                id = newChild.id,
                fullName = newChild.studentName,
                firstName = newChild.studentName.split(" ").firstOrNull() ?: newChild.studentName,
                lastName = newChild.studentName.split(" ").drop(1).joinToString(" ")
            )
        }

        val updatedSession = current.session.copy(
            activeChild = newChild,
            profile = newProfile
        )
        _state.value = AuthState.Authenticated(updatedSession)

        // Update selectedChildId in persistent storage
        storage.getString(KEY_SAVED_CREDENTIALS)?.let {
            try {
                val creds = json.decodeFromString<SavedCredentials>(it)
                val updatedCreds = creds.copy(selectedChildId = childId)
                storage.putString(KEY_SAVED_CREDENTIALS, json.encodeToString(updatedCreds))
            } catch (_: Exception) {}
        }
    }

    suspend fun loginPortal(email: String, pass: String, portalClient: PortalAuthClient) {
        _state.value = AuthState.Authenticating("Łączenie z Portal Rodzina...")
        try {
            val children = portalClient.login(email, pass)
            if (children.isEmpty()) {
                _state.value = AuthState.Error("Brak powiązanych kont uczniów w koncie Librus Portal.")
                return
            }
            val active = children.first()
            val profile = StudentProfile(
                id = active.id,
                firstName = active.studentName.split(" ").firstOrNull() ?: active.studentName,
                lastName = active.studentName.split(" ").drop(1).joinToString(" "),
                fullName = active.studentName,
                accountEmail = email
            )
            val session = UserSession(
                backendType = AuthBackendType.PORTAL_RODZINA,
                identifier = email,
                children = children,
                activeChild = active,
                profile = profile
            )
            _state.value = AuthState.Authenticated(session)

            val creds = SavedCredentials.create(
                backendType = AuthBackendType.PORTAL_RODZINA,
                identifier = email,
                rawPassword = pass,
                selectedChildId = active.id
            )
            storage.putString(KEY_SAVED_CREDENTIALS, json.encodeToString(creds))
        } catch (e: Exception) {
            _state.value = AuthState.Error(e.message ?: "Wystąpił nieoczekiwany błąd podczas logowania.")
        }
    }

    suspend fun loginGateway(login: String, pass: String, gatewayClient: GatewayAuthClient) {
        _state.value = AuthState.Authenticating("Logowanie do Synergia Gateway...")
        try {
            val gatewaySession = gatewayClient.login(login, pass)
            val active = gatewaySession.childAccount
            val profile = StudentProfile(
                id = active.id,
                firstName = active.studentName,
                lastName = "",
                fullName = active.studentName,
                className = gatewaySession.className
            )
            val session = UserSession(
                backendType = AuthBackendType.GATEWAY_API_20,
                identifier = login,
                children = listOf(active),
                activeChild = active,
                profile = profile
            )
            _state.value = AuthState.Authenticated(session)

            val creds = SavedCredentials.create(
                backendType = AuthBackendType.GATEWAY_API_20,
                identifier = login,
                rawPassword = pass,
                selectedChildId = active.id
            )
            storage.putString(KEY_SAVED_CREDENTIALS, json.encodeToString(creds))
        } catch (e: Exception) {
            _state.value = AuthState.Error(e.message ?: "Błąd logowania do systemu Synergia.")
        }
    }

    fun logout() {
        storage.remove(KEY_SAVED_CREDENTIALS)
        storage.remove(KEY_CACHED_STATE)
        _state.value = AuthState.Unauthenticated
    }

    fun clearError() {
        _state.value = AuthState.Unauthenticated
    }

    companion object {
        const val KEY_SAVED_CREDENTIALS = "saved_credentials"
        const val KEY_CACHED_STATE = "cached_app_state"
    }
}
