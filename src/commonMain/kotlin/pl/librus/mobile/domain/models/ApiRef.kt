package pl.librus.mobile.domain.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Defensive reference wrapper for Librus API objects.
 * Handles:
 * - Object: {"Id": 42, "Url": "..."}
 * - Object: {"Id": "42"}
 * - Raw primitive: 42 or "42"
 * - Null
 */
@Serializable(with = ApiRefSerializer::class)
data class ApiRef(
    val id: String? = null,
    val url: String? = null
) {
    val resolvedId: String?
        get() = id ?: url?.substringAfterLast('/')?.takeIf { it.isNotEmpty() }
}

object ApiRefSerializer : KSerializer<ApiRef> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ApiRef", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ApiRef {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return ApiRef(id = decoder.decodeString())

        val element = jsonDecoder.decodeJsonElement()
        return when {
            element is JsonObject -> {
                val idElem = element["Id"] ?: element["id"]
                val idStr = when (idElem) {
                    is JsonPrimitive -> idElem.content
                    else -> null
                }
                val urlElem = element["Url"] ?: element["url"]
                val urlStr = when (urlElem) {
                    is JsonPrimitive -> urlElem.content
                    else -> null
                }
                ApiRef(id = idStr, url = urlStr)
            }
            element is JsonPrimitive -> {
                ApiRef(id = element.contentOrNull)
            }
            else -> ApiRef()
        }
    }

    override fun serialize(encoder: Encoder, value: ApiRef) {
        encoder.encodeString(value.resolvedId ?: "")
    }
}
