package pl.librus.mobile.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class LibrusMessage(
    val id: String,
    val subject: String,
    val body: String,
    val senderName: String,
    val senderId: String? = null,
    val date: String,
    val isRead: Boolean = false,
    val hasAttachments: Boolean = false,
    val attachments: List<MessageAttachment> = emptyList(),
    val isSentByMe: Boolean = false
)

@Serializable
data class MessageAttachment(
    val id: String,
    val name: String,
    val size: Long = -1
)
