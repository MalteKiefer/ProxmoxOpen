package de.kiefer_networks.proxmoxopen.ui.format

/**
 * Parses a Proxmox disk config string like
 * "local-lvm:vm-100-disk-0,size=32G,discard=on,iothread=1"
 * into a structured display.
 */
data class ParsedDisk(
    val storage: String,
    val volume: String,
    val size: String?,
    val format: String?,
    val cache: String?,
    val discard: Boolean,
    val ssd: Boolean,
    val iothread: Boolean,
    val backup: Boolean,
)

fun parseDiskString(raw: String): ParsedDisk {
    val parts = raw.split(",")
    val storageVolume = parts.firstOrNull()?.trim() ?: raw
    val (storage, volume) = if (storageVolume.contains(":")) {
        storageVolume.substringBefore(":") to storageVolume.substringAfter(":")
    } else {
        "" to storageVolume
    }
    val kv = parts.drop(1).associate { p ->
        val eq = p.indexOf('=')
        if (eq > 0) p.substring(0, eq).trim() to p.substring(eq + 1).trim()
        else p.trim() to "1"
    }
    return ParsedDisk(
        storage = storage,
        volume = volume,
        size = kv["size"],
        format = kv["format"],
        cache = kv["cache"],
        discard = kv["discard"] == "on",
        ssd = kv["ssd"] == "1",
        iothread = kv["iothread"] == "1",
        backup = kv["backup"] != "0",
    )
}

fun ParsedDisk.displayLine(): String = buildString {
    append(storage)
    size?.let { append(" · $it") }
    format?.let { append(" · $it") }
    if (discard) append(" · TRIM")
    if (ssd) append(" · SSD")
    if (iothread) append(" · IOthread")
    cache?.let { append(" · cache=$it") }
}
