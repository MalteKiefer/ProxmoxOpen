package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.BackupJob

interface BackupJobRepository {
    suspend fun listJobs(serverId: Long): List<BackupJob>
    suspend fun runJob(serverId: Long, id: String)
    suspend fun createJob(serverId: Long, params: Map<String, String>)
    suspend fun updateJob(serverId: Long, id: String, params: Map<String, String>)
    suspend fun deleteJob(serverId: Long, id: String)
    suspend fun listBackupStorages(serverId: Long, node: String): List<String>
}
