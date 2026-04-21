package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * One entry returned by `GET /nodes/{node}/disks/list`. Fields are optional because
 * Proxmox omits many of them for unmounted / unknown devices.
 *
 * [wearout] is kept as [JsonElement] because Proxmox returns either an integer percentage
 * or the string "N/A" depending on the drive.
 */
@Serializable
data class NodeDiskDto(
    val devpath: String,
    val model: String? = null,
    val serial: String? = null,
    val size: Long? = null,
    val used: String? = null,
    val vendor: String? = null,
    val health: String? = null,
    val rpm: Long? = null,
    val type: String? = null,
    val wearout: JsonElement? = null,
    val wwn: String? = null,
    val mounted: Int? = null,
    val gpt: Int? = null,
    @SerialName("osdid") val osdId: Int? = null,
) {
    /** Returns the wearout as a display string (e.g. "96%") or null if absent / not applicable. */
    fun wearoutPercent(): Int? {
        val raw = (wearout as? JsonPrimitive) ?: return null
        val asLong = raw.contentOrNull?.toLongOrNull()
        return asLong?.toInt()
    }
}
