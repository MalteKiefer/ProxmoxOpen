package app.proxmoxopen.domain.usecase

import app.proxmoxopen.domain.repository.AuthRepository
import app.proxmoxopen.domain.repository.ServerProbe
import app.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class ProbeServerUseCase @Inject constructor(
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(host: String, port: Int): ApiResult<ServerProbe> =
        auth.probeFingerprint(host, port)
}
