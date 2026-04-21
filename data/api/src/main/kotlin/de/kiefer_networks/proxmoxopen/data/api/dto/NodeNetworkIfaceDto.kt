package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Entry returned by `GET /nodes/{n}/network` — one per host network interface
 * (bridge, bond, physical NIC, VLAN, etc).
 *
 * This is strictly used for read-only display. Editing interfaces on a host from
 * a mobile device is risky and not supported by this app.
 */
@Serializable
data class NodeNetworkIfaceDto(
    @SerialName("iface") val iface: String,
    @SerialName("type") val type: String? = null,
    @SerialName("active") val active: Int? = null,
    @SerialName("method") val method: String? = null,
    @SerialName("method6") val method6: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("netmask") val netmask: String? = null,
    @SerialName("gateway") val gateway: String? = null,
    @SerialName("address6") val address6: String? = null,
    @SerialName("netmask6") val netmask6: String? = null,
    @SerialName("gateway6") val gateway6: String? = null,
    @SerialName("cidr") val cidr: String? = null,
    @SerialName("cidr6") val cidr6: String? = null,
    @SerialName("autostart") val autostart: Int? = null,
    @SerialName("bridge_ports") val bridgePorts: String? = null,
    @SerialName("bridge_stp") val bridgeStp: String? = null,
    @SerialName("bridge_fd") val bridgeFd: String? = null,
    @SerialName("bond_mode") val bondMode: String? = null,
    @SerialName("bond_miimon") val bondMiimon: String? = null,
    @SerialName("slaves") val slaves: String? = null,
    @SerialName("vlan-id") val vlanId: Int? = null,
    @SerialName("vlan-raw-device") val vlanRawDevice: String? = null,
    @SerialName("mtu") val mtu: String? = null,
    @SerialName("comments") val comments: String? = null,
)
