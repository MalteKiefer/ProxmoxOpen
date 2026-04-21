package de.kiefer_networks.proxmoxopen.domain.model

/** Summary of a physical disk as returned by `GET /nodes/{n}/disks/list`. */
data class DiskInfo(
    val devpath: String,
    val model: String?,
    val serial: String?,
    val vendor: String?,
    val size: Long,
    val type: DiskType,
    val health: DiskHealth,
    val wearoutPercent: Int?,
    val rpm: Long?,
    val wwn: String?,
    val used: String?,
    val mounted: Boolean,
    val gpt: Boolean,
    val osdId: Int?,
)

enum class DiskType { SSD, HDD, NVME, USB, UNKNOWN;
    companion object {
        fun fromApi(raw: String?): DiskType = when (raw?.lowercase()) {
            "ssd" -> SSD
            "hdd" -> HDD
            "nvme" -> NVME
            "usb" -> USB
            else -> UNKNOWN
        }
    }
}

enum class DiskHealth { PASSED, FAILED, UNKNOWN;
    companion object {
        fun fromApi(raw: String?): DiskHealth = when (raw?.uppercase()) {
            "PASSED", "OK" -> PASSED
            "FAILED", "FAIL" -> FAILED
            else -> UNKNOWN
        }
    }
}

/** Parsed SMART report for a single disk. */
data class SmartReport(
    val health: DiskHealth,
    val type: String?,
    val text: String?,
    val attributes: List<SmartAttributeEntry>,
)

data class SmartAttributeEntry(
    val name: String,
    val value: String,
    val rawValue: String?,
)
