package app.proxmoxopen.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subset of the LXC/QEMU config returned by `GET /nodes/{n}/{type}/{vmid}/config`.
 * Only the fields we allow editing are listed; the rest is ignored by kotlinx.serialization.
 */
@Serializable
data class GuestConfigDto(
    // Common
    val name: String? = null,
    val onboot: Int? = null,
    val startup: String? = null,          // "order=N,up=N,down=N"
    val description: String? = null,
    val tags: String? = null,

    // LXC-specific
    val hostname: String? = null,
    val nameserver: String? = null,
    val searchdomain: String? = null,

    // Network: Proxmox returns net0, net1, ... as top-level keys.
    val net0: String? = null,
    val net1: String? = null,
    val net2: String? = null,
    val net3: String? = null,

    // QEMU: boot order
    val boot: String? = null,

    // LXC: cores + memory
    val cores: Int? = null,
    val memory: Int? = null,       // MB
    val swap: Int? = null,         // MB

    // QEMU: sockets + cores + memory
    val sockets: Int? = null,
    @SerialName("balloon") val balloon: Int? = null,
)
