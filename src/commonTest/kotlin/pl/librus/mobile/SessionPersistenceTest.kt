package pl.librus.mobile

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.librus.mobile.auth.AuthBackendType
import pl.librus.mobile.auth.AuthManager
import pl.librus.mobile.auth.AuthState
import pl.librus.mobile.data.storage.CachedAppState
import pl.librus.mobile.data.storage.MemoryPersistentStorage
import pl.librus.mobile.data.storage.SavedCredentials
import pl.librus.mobile.domain.models.ChildAccount
import pl.librus.mobile.domain.models.StudentProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionPersistenceTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun testSavedCredentialsObfuscation() {
        val creds = SavedCredentials.create(
            backendType = AuthBackendType.PORTAL_RODZINA,
            identifier = "rodzic@librus.pl",
            rawPassword = "SecretPassword123!",
            selectedChildId = "child_1"
        )

        // Password should not be stored in plain text
        assertTrue(creds.passwordObfuscated != "SecretPassword123!")
        // Deobfuscation recovers original password
        assertEquals("SecretPassword123!", creds.password)
        assertEquals("rodzic@librus.pl", creds.identifier)
        assertEquals("child_1", creds.selectedChildId)
    }

    @Test
    fun testAuthManagerRestoresSessionImmediatelyWithoutLoginScreen() {
        val storage = MemoryPersistentStorage()

        // 1. Pre-seed storage as if a user was previously logged in
        val savedCreds = SavedCredentials.create(
            backendType = AuthBackendType.PORTAL_RODZINA,
            identifier = "rodzic@librus.pl",
            rawPassword = "SecretPassword123!",
            selectedChildId = "1001"
        )
        storage.putString(AuthManager.KEY_SAVED_CREDENTIALS, json.encodeToString(savedCreds))

        val cachedProfile = StudentProfile(
            id = "1001",
            firstName = "Jan",
            lastName = "Kowalski",
            fullName = "Jan Kowalski",
            className = "3B"
        )
        val cachedChild = ChildAccount(
            id = "1001",
            accountIdentifier = "1234567u",
            studentName = "Jan Kowalski",
            login = "1234567u",
            accessToken = "cached_token"
        )
        val cachedState = CachedAppState(
            profile = cachedProfile,
            children = listOf(cachedChild),
            activeChild = cachedChild
        )
        storage.putString(AuthManager.KEY_CACHED_STATE, json.encodeToString(cachedState))

        // 2. Instantiate AuthManager (simulating app cold launch)
        val authManager = AuthManager(storage)

        // 3. Immediately assert state is Authenticated, with remembered user, so login screen is bypassed!
        val state = authManager.state.value
        assertIs<AuthState.Authenticated>(state)
        assertEquals("1001", state.session.activeChild.id)
        assertEquals("Jan Kowalski", state.session.profile.fullName)
        assertEquals("3B", state.session.profile.className)
    }

    @Test
    fun testAuthManagerLogoutClearsStorage() {
        val storage = MemoryPersistentStorage()
        val authManager = AuthManager(storage)

        authManager.loginDemo()
        assertIs<AuthState.Authenticated>(authManager.state.value)
        assertNotNull(storage.getString(AuthManager.KEY_SAVED_CREDENTIALS))

        authManager.logout()
        assertIs<AuthState.Unauthenticated>(authManager.state.value)
        assertTrue(storage.getString(AuthManager.KEY_SAVED_CREDENTIALS) == null)
        assertTrue(storage.getString(AuthManager.KEY_CACHED_STATE) == null)
    }
}
