package de.kiefer_networks.proxmoxopen.domain.model

/**
 * Result of a global cluster search. One entry per matched resource from
 * `/cluster/resources`. Guests, nodes and storages are represented as distinct
 * sub-types so the UI can render + route them without string-typing.
 */
sealed class SearchResult {
    /** The lower-cased id from the Proxmox response (e.g. `qemu/100`, `node/pve1`). Unique per cluster. */
    abstract val id: String

    data class GuestResult(
        override val id: String,
        val vmid: Int,
        val name: String,
        val node: String,
        val type: GuestType,
        val status: GuestStatus,
        val tags: List<String>,
    ) : SearchResult()

    data class NodeResult(
        override val id: String,
        val node: String,
        val status: String?,
    ) : SearchResult()

    data class StorageResult(
        override val id: String,
        val storage: String,
        val node: String,
        val content: String?,
        val status: String?,
    ) : SearchResult()
}
