package app.proxmoxopen.domain.model

data class GuestConfig(
    val name: String,
    val hostname: String?,
    val onboot: Boolean,
    val startup: String?,
    val description: String?,
    val nameserver: String?,
    val searchdomain: String?,
    val cores: Int?,
    val memory: Int?,
    val swap: Int?,
    val networkInterfaces: List<NetworkInterface>,
)

data class NetworkInterface(
    val id: String,       // "net0", "net1", ...
    val rawValue: String, // full Proxmox config string
    val bridge: String?,
    val hwaddr: String?,
    val ip: String?,
    val gw: String?,
    val ip6: String?,
    val gw6: String?,
    val firewall: Boolean,
    val name: String?,    // eth0, etc. (LXC)
) {
    companion object {
        fun parse(id: String, raw: String): NetworkInterface {
            val kv = raw.split(",").associate { part ->
                val eq = part.indexOf('=')
                if (eq > 0) part.substring(0, eq) to part.substring(eq + 1)
                else part to ""
            }
            return NetworkInterface(
                id = id,
                rawValue = raw,
                bridge = kv["bridge"],
                hwaddr = kv["hwaddr"],
                ip = kv["ip"],
                gw = kv["gw"],
                ip6 = kv["ip6"],
                gw6 = kv["gw6"],
                firewall = kv["firewall"] == "1",
                name = kv["name"],
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
        return parts.joinToString(",")
    }
}
