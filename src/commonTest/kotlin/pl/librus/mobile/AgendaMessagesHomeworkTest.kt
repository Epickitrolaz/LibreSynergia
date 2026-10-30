package pl.librus.mobile

import kotlinx.coroutines.runBlocking
import pl.librus.mobile.auth.AuthBackendType
import pl.librus.mobile.auth.UserSession
import pl.librus.mobile.data.LibrusRepository
import pl.librus.mobile.data.MockDataProvider
import pl.librus.mobile.data.storage.MemoryPersistentStorage
import pl.librus.mobile.domain.models.AgendaEventType
import pl.librus.mobile.domain.models.ChildAccount
import pl.librus.mobile.domain.models.StudentProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgendaMessagesHomeworkTest {

    private fun createDemoSession(): UserSession {
        val child = ChildAccount(
            id = "1001",
            accountIdentifier = "1234567u",
            studentName = "Jan Kowalski",
            login = "1234567u",
            accessToken = "demo_token",
            state = "active"
        )
        return UserSession(
            backendType = AuthBackendType.DEMO_MOCK,
            identifier = "1234567u",
            children = listOf(child),
            activeChild = child,
            profile = StudentProfile("1001", "Jan", "Kowalski")
        )
    }

    @Test
    fun testMessageReadStatusPersistence() = runBlocking {
        val storage = MemoryPersistentStorage()
        val repo1 = LibrusRepository(storage, liveApiClient = null)
        val session = createDemoSession()

        repo1.loadDataForSession(session)

        // Verify initial mock messages exist and m1 is unread
        val m1Before = repo1.messages.value.find { it.id == "m1" }
        assertFalse(m1Before?.isRead ?: true)

        // Mark m1 as read
        repo1.markMessageAsRead("m1")
        val m1After = repo1.messages.value.find { it.id == "m1" }
        assertTrue(m1After?.isRead ?: false)

        // Create new repository instance with the same storage (simulating app restart)
        val repo2 = LibrusRepository(storage, liveApiClient = null)
        val m1Restored = repo2.messages.value.find { it.id == "m1" }
        assertTrue(m1Restored?.isRead ?: false, "Message read status must persist in storage across restarts")
    }

    @Test
    fun testHomeworkCompletionTogglePersistence() = runBlocking {
        val storage = MemoryPersistentStorage()
        val repo1 = LibrusRepository(storage, liveApiClient = null)
        val session = createDemoSession()

        repo1.loadDataForSession(session)

        val hw1Before = repo1.homeworks.value.find { it.id == "hw1" }
        assertFalse(hw1Before?.isDone ?: true)

        // Toggle homework 1 to done
        repo1.toggleHomeworkDone("hw1")
        val hw1After = repo1.homeworks.value.find { it.id == "hw1" }
        assertTrue(hw1After?.isDone ?: false)

        // Create new repository instance with the same storage
        val repo2 = LibrusRepository(storage, liveApiClient = null)
        val hw1Restored = repo2.homeworks.value.find { it.id == "hw1" }
        assertTrue(hw1Restored?.isDone ?: false, "Homework done status must persist in storage across restarts")

        // Toggle back to undone
        repo2.toggleHomeworkDone("hw1")
        assertFalse(repo2.homeworks.value.find { it.id == "hw1" }?.isDone ?: true)
    }

    @Test
    fun testAgendaEventsChronologicalOrdering() = runBlocking {
        val storage = MemoryPersistentStorage()
        val repo = LibrusRepository(storage, liveApiClient = null)
        val session = createDemoSession()

        repo.loadDataForSession(session)

        val events = repo.agendaEvents.value
        assertTrue(events.isNotEmpty(), "Agenda events should be loaded")

        // Verify chronological order: each event's date is >= previous event's date
        for (i in 0 until events.size - 1) {
            val curr = events[i]
            val next = events[i + 1]
            assertTrue(
                curr.date <= next.date,
                "Agenda events must be ordered chronologically: ${curr.date} should be <= ${next.date}"
            )
        }

        // Verify event types exist
        assertTrue(events.any { it.type == AgendaEventType.EXAM }, "Agenda should contain exams")
        assertTrue(events.any { it.type == AgendaEventType.QUIZ }, "Agenda should contain quizzes")
    }
}
