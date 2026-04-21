package de.kiefer_networks.proxmoxopen.domain.model

/** Coarse category of a host network interface. */
enum class NodeNetworkIfaceType {
    BRIDGE,
    BOND,
    ETH,
    VLAN,
    LOOPBACK,
    OTHER;

    companion object {
        fun fromProxmox(raw: String?): NodeNetworkIfaceType = when (raw?.lowercase()) {
            "bridge" -> BRIDGE
            "bond" -> BOND
            "eth" -> ETH
            "vlan" -> VLAN
            "loopback", "lo" -> LOOPBACK
            else -> OTHER
        }
    }
}

/**
 * Domain model for a host network interface as returned by
 * `GET /nodes/{n}/network`.
 *
 * All fields are read-only for UI display. The app does not provide any
 * create/edit/delete pathway for these — users must use the Proxmox web UI
 * for network edits.
 */
data class NodeNetworkIface(
    val iface: String,
    val type: NodeNetworkIfaceType,
    val rawType: String?,
    val active: Boolean?,
    val autostart: Boolean?,
    val method: String?,
    val method6: String?,
    val address: String?,
    val netmask: String?,
    val gateway: String?,
    val address6: String?,
    val netmask6: String?,
    val gateway6: String?,
    val cidr: String?,
    val cidr6: String?,
    val bridgePorts: List<String>,
    val bridgeStp: String?,
    val bridgeFd: String?,
    val bondMode: String?,
    val bondMiimon: String?,
    val slaves: List<String>,
    val vlanId: Int?,
    val vlanRawDevice: String?,
    val mtu: String?,
    val comments: String?,
)
