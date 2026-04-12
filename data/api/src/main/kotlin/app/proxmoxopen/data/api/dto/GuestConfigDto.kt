package app.proxmoxopen.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * LXC container config from `GET /nodes/{n}/lxc/{vmid}/config`.
 * Proxmox returns net0..net31 as flat top-level keys. We map the first 16
 * (net0..net15) here; more can be added if needed.
 */
@Serializable
data class GuestConfigDto(
    // General
    val hostname: String? = null,
    val name: String? = null,
    val description: String? = null,
    val tags: String? = null,
    val onboot: Int? = null,
    val startup: String? = null,
    val protection: Int? = null,
    val unprivileged: Int? = null,
    val features: String? = null,
    val arch: String? = null,
    val ostype: String? = null,

    // Resources
    val cores: Int? = null,
    val cpulimit: Double? = null,
    val cpuunits: Int? = null,
    val memory: Int? = null,        // MB
    val swap: Int? = null,          // MB

    // DNS
    val nameserver: String? = null,
    val searchdomain: String? = null,

    // Network interfaces
    val net0: String? = null,
    val net1: String? = null,
    val net2: String? = null,
    val net3: String? = null,
    val net4: String? = null,
    val net5: String? = null,
    val net6: String? = null,
    val net7: String? = null,
    val net8: String? = null,
    val net9: String? = null,
    val net10: String? = null,
    val net11: String? = null,
    val net12: String? = null,
    val net13: String? = null,
    val net14: String? = null,
    val net15: String? = null,

    // Rootfs / mounts
    val rootfs: String? = null,
    val mp0: String? = null,
    val mp1: String? = null,
    val mp2: String? = null,
    val mp3: String? = null,

    // QEMU compat (ignored for LXC)
    val boot: String? = null,
    val sockets: Int? = null,
    @SerialName("balloon") val balloon: Int? = null,
) {
    /** Collects all non-null net[n] values into an ordered list of (id, raw). */
    fun allNetworkInterfaces(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val fields = listOf(
            "net0" to net0, "net1" to net1, "net2" to net2, "net3" to net3,
            "net4" to net4, "net5" to net5, "net6" to net6, "net7" to net7,
            "net8" to net8, "net9" to net9, "net10" to net10, "net11" to net11,
            "net12" to net12, "net13" to net13, "net14" to net14, "net15" to net15,
        )
        for ((id, value) in fields) {
            if (value != null) result += id to value
        }
        return result
    }
}
