package de.kiefer_networks.proxmoxopen.domain.model

enum class RrdTimeframe(val apiKey: String) {
    HOUR("hour"),
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
}

data class RrdPoint(
    val time: Long,
    val cpu: Double?,
    val memUsed: Double?,
    val memTotal: Double?,
    val netIn: Double?,
    val netOut: Double?,
    val diskRead: Double?,
    val diskWrite: Double?,
    val ioWait: Double? = null,
)
