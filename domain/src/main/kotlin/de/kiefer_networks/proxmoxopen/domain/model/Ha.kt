package de.kiefer_networks.proxmoxopen.domain.model

/**
 * Aggregated HA cluster status derived from `/cluster/ha/status/current`.
 * The raw endpoint returns a list of heterogeneous entries; this model
 * presents them as a view that is easy to render.
 */
data class HaClusterStatus(
    /** Whether the cluster currently has quorum. `null` if unknown. */
    val quorate: Boolean?,
    /** Node name that holds the HA master role, if known. */
    val master: String?,
    /** HA-enabled cluster members (quorum and node entries). */
    val members: List<HaMember>,
)

/**
 * A single line as reported by `/cluster/ha/status/current`. Entries of type
 * `quorum`, `master`, `node` and `service` are all surfaced as members so the
 * UI can render the raw status list without losing information.
 */
data class HaMember(
    val id: String,
    val type: String,
    val node: String?,
    val status: String?,
    val state: String?,
    val timestamp: Long?,
)

/** An HA-managed guest (VM / LXC) from `/cluster/ha/resources`. */
data class HaResource(
    val sid: String,
    val type: String,
    val state: String,
    val group: String?,
    val comment: String?,
    val maxRelocate: Int?,
    val maxRestart: Int?,
)

/** An HA failover group from `/cluster/ha/groups`. */
data class HaGroup(
    val group: String,
    val nodes: List<String>,
    val comment: String?,
    val restricted: Boolean,
    val nofailback: Boolean,
    val type: String?,
)
