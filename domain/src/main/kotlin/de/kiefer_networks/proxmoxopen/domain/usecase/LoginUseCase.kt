package de.kiefer_networks.proxmoxopen.domain.usecase

import de.kiefer_networks.proxmoxopen.domain.model.AuthSession
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.repository.AuthRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val servers: ServerRepository,
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(serverId: Long, credentials: Credentials): ApiResult<AuthSession> {
        val server = servers.getById(serverId)
            ?: return ApiResult.Failure(ApiError.Unknown("Server $serverId not found"))
        return auth.login(server, credentials).also {
            if (it is ApiResult.Success) servers.touchLastConnected(serverId)
        }
    }
}
