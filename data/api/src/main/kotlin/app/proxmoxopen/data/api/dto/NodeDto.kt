package app.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class NodeListDto(
    val node: String,
    val status: String,
    val cpu: Double? = null,
    val maxcpu: Int? = null,
    val mem: Long? = null,
    val maxmem: Long? = null,
    val disk: Long? = null,
    val maxdisk: Long? = null,
    val uptime: Long? = null,
    val level: String? = null,
)

@Serializable
data class NodeStatusDto(
    val uptime: Long? = null,
    val cpu: Double? = null,
    val cpuinfo: CpuInfoDto? = null,
    val loadavg: List<String>? = null,
    val memory: MemoryDto? = null,
    val rootfs: StorageDto? = null,
    val swap: StorageDto? = null,
    val ksm: KsmDto? = null,
    val kversion: String? = null,
    val pveversion: String? = null,
    val iowait: Double? = null,
    val bootmode: String? = null,
)

@Serializable
data class CpuInfoDto(val cpus: Int? = null, val model: String? = null)

@Serializable
data class MemoryDto(val total: Long? = null, val used: Long? = null, val free: Long? = null)

@Serializable
data class StorageDto(val total: Long? = null, val used: Long? = null, val free: Long? = null, val avail: Long? = null)

@Serializable
data class KsmDto(val shared: Long? = null)
