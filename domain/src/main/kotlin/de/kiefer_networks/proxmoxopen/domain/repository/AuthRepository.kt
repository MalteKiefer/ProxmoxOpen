package de.kiefer_networks.proxmoxopen.domain.repository

import de.kiefer_networks.proxmoxopen.domain.model.AuthSession
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.Server

interface AuthRepository {
    suspend fun login(server: Server, credentials: Credentials): ApiResult<AuthSession>
    suspend fun currentSession(serverId: Long): AuthSession?
    suspend fun logout(serverId: Long)

    /** Probes a server's certificate fingerprint without authenticating. */
    suspend fun probeFingerprint(host: String, port: Int): ApiResult<ServerProbe>
}

data class ServerProbe(
    val host: String,
    val port: Int,
    val subject: String,
    val issuer: String,
    val validFrom: Long,
    val validTo: Long,
    val sha256Fingerprint: String,
)
