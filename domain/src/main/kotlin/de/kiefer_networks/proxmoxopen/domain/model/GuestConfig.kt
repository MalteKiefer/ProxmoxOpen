package de.kiefer_networks.proxmoxopen.domain.model

data class GuestConfig(
    val name: String,
    val hostname: String?,
    val onboot: Boolean,
    val startup: String?,
    val description: String?,
    val protection: Boolean,
    val unprivileged: Boolean,
    // Resources
    val cores: Int?,
    val cpulimit: Double?,
    val cpuunits: Int?,
    val memory: Int?,     // MB
    val swap: Int?,       // MB
    // DNS
    val nameserver: String?,     // comma-separated
    val searchdomain: String?,
    // Network (net0..net31)
    val networkInterfaces: List<NetworkInterface>,
    // Tags
    val tags: String?,
    // Features
    val features: String?,
    val arch: String?,
    val ostype: String?,
)

data class NetworkInterface(
    val id: String,           // "net0", "net1", ...
    val rawValue: String,
    val name: String?,        // veth name inside CT (eth0, etc.)
    val bridge: String?,
    val hwaddr: String?,
    val ip: String?,          // "dhcp", "manual", or "x.x.x.x/cidr"
    val gw: String?,
    val ip6: String?,         // "dhcp", "auto", "manual", or "x::x/cidr"
    val gw6: String?,
    val firewall: Boolean,
    val mtu: Int?,
    val rate: Double?,        // MB/s rate limit
    val tag: Int?,            // VLAN tag
    val type: String?,        // "veth" usually
    val linkDown: Boolean,
) {
    companion object {
        fun parse(id: String, raw: String): NetworkInterface {
            val kv = raw.split(",").associate { part ->
                val eq = part.indexOf('=')
                if (eq > 0) part.substring(0, eq).trim() to part.substring(eq + 1).trim()
                else part.trim() to ""
            }
            return NetworkInterface(
                id = id,
                rawValue = raw,
                name = kv["name"],
                bridge = kv["bridge"],
                hwaddr = kv["hwaddr"],
                ip = kv["ip"],
                gw = kv["gw"],
                ip6 = kv["ip6"],
                gw6 = kv["gw6"],
                firewall = kv["firewall"] == "1",
                mtu = kv["mtu"]?.toIntOrNull(),
                rate = kv["rate"]?.toDoubleOrNull(),
                tag = kv["tag"]?.toIntOrNull(),
                type = kv["type"],
                linkDown = kv["link_down"] == "1",
            )
        }
    }

    fun toProxmoxString(): String {
        val parts = mutableListOf<String>()
        name?.let { parts += "name=$it" }
        bridge?.let { parts += "bridge=$it" }
        hwaddr?.let { parts += "hwaddr=$it" }
        ip?.let { parts += "ip=$it" }
        gw?.let { parts += "gw=$it" }
        ip6?.let { parts += "ip6=$it" }
        gw6?.let { parts += "gw6=$it" }
        if (firewall) parts += "firewall=1"
        mtu?.let { parts += "mtu=$it" }
        rate?.let { parts += "rate=$it" }
        tag?.let { parts += "tag=$it" }
        type?.let { parts += "type=$it" }
        if (linkDown) parts += "link_down=1"
        return parts.joinToString(",")
    }
}
