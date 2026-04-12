package de.kiefer_networks.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RrdPointDto(
    val time: Long,
    val cpu: Double? = null,
    // VMs/CTs use "mem" / "maxmem"; nodes use "memused" / "memtotal".
    val mem: Double? = null,
    val memused: Double? = null,
    val maxmem: Double? = null,
    val memtotal: Double? = null,
    val netin: Double? = null,
    val netout: Double? = null,
    val diskread: Double? = null,
    val diskwrite: Double? = null,
    // Nodes also return root-fs I/O; VMs return disk I/O.
    val rootused: Double? = null,
    val roottotal: Double? = null,
    val iowait: Double? = null,
    val loadavg: Double? = null,
    val swapused: Double? = null,
    val swaptotal: Double? = null,
)
