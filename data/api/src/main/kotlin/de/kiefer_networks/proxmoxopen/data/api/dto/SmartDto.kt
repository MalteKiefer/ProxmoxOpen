package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Defensive DTO for `GET /nodes/{node}/disks/smart`.
 *
 * Proxmox returns two different shapes depending on the underlying drive type:
 *
 * * ATA / SATA — `attributes` is a list of `{id, name, value, worst, raw, flags, ...}` objects.
 * * NVMe      — the body exposes a flat key/value map (`critical_warning`, `temperature`, …).
 *
 * We parse both permissively and flatten them into a single [Map] of strings so the UI can
 * render a simple attributes table without having to know the disk type.
 */
@Serializable
data class SmartDto(
    val health: String? = null,
    val type: String? = null,
    val text: String? = null,
    val attributes: JsonElement? = null,
) {
    /** Flattens both shapes (ATA list / NVMe map) into `name -> value` pairs for the UI. */
    fun attributePairs(): List<SmartAttribute> = buildList {
        when (val attr = attributes) {
            is JsonArray -> attr.forEach { el ->
                val obj = el as? JsonObject ?: return@forEach
                val name = obj["name"]?.asStringOrNull() ?: obj["id"]?.asStringOrNull() ?: return@forEach
                val value = obj["value"]?.asStringOrNull() ?: ""
                val raw = obj["raw"]?.asStringOrNull() ?: obj["rawvalue"]?.asStringOrNull()
                add(SmartAttribute(name = name, value = value, rawValue = raw))
            }
            is JsonObject -> attr.entries.forEach { (k, v) ->
                add(SmartAttribute(name = k, value = v.asStringOrNull() ?: "", rawValue = null))
            }
            else -> {}
        }
    }
}

data class SmartAttribute(
    val name: String,
    val value: String,
    val rawValue: String?,
)

private fun JsonElement.asStringOrNull(): String? = when (this) {
    is JsonPrimitive -> contentOrNull
    is JsonObject -> jsonObject.toString()
    else -> toString()
}
