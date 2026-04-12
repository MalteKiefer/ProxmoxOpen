package de.kiefer_networks.proxmoxopen.domain.model

data class VmConfig(
    val name: String,
    val description: String?,
    val onboot: Boolean,
    val startup: String?,
    val protection: Boolean,
    // Hardware
    val bios: String?,
    val machine: String?,
    val cpuType: String?,
    val sockets: Int?,
    val cores: Int?,
    val memory: Int?,
    val balloon: Int?,
    val numa: Boolean,
    val scsihw: String?,
    val ostype: String?,
    val vga: String?,
    val agentEnabled: Boolean,
    val bootOrder: String?,
    // Disks (id -> raw config string)
    val disks: List<Pair<String, String>>,
    // Network (id -> raw config string, parsed as VmNetInterface)
    val networkInterfaces: List<VmNetInterface>,
    val tags: String?,
)

data class VmNetInterface(
    val id: String,
    val rawValue: String,
    val model: String?,      // virtio, e1000, e1000e, rtl8139
    val bridge: String?,
    val macaddr: String?,
    val firewall: Boolean,
    val rate: Double?,
    val tag: Int?,
    val queues: Int?,
    val mtu: Int?,
) {
    companion object {
        fun parse(id: String, raw: String): VmNetInterface {
            val kv = raw.split(",").associate { p ->
                val eq = p.indexOf('=')
                if (eq > 0) p.substring(0, eq).trim() to p.substring(eq + 1).trim()
                else p.trim() to ""
            }
            // First key without = is often the model (e.g. "virtio=XX:XX:XX:XX:XX:XX")
            val modelEntry = raw.split(",").firstOrNull { !it.contains("=") || it.split("=")[0] in listOf("virtio", "e1000", "e1000e", "rtl8139", "vmxnet3") }
            val model = modelEntry?.split("=")?.firstOrNull()
            val macaddr = modelEntry?.split("=")?.getOrNull(1) ?: kv["macaddr"]

            return VmNetInterface(
                id = id,
                rawValue = raw,
                model = model,
                bridge = kv["bridge"],
                macaddr = macaddr,
                firewall = kv["firewall"] == "1",
                rate = kv["rate"]?.toDoubleOrNull(),
                tag = kv["tag"]?.toIntOrNull(),
                queues = kv["queues"]?.toIntOrNull(),
                mtu = kv["mtu"]?.toIntOrNull(),
            )
        }
    }
}
