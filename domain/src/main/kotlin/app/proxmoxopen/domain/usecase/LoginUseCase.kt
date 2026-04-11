package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.model.AuthSession
import app.proxmoxopen.domain.model.Credentials
import app.proxmoxopen.domain.repository.AuthRepository
import app.proxmoxopen.domain.repository.ServerRepository
import app.proxmoxopen.domain.result.ApiError
import app.proxmoxopen.domain.result.ApiResult
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
