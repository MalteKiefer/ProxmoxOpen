package de.kiefer_networks.proxmoxopen.domain.usecase

import de.kiefer_networks.proxmoxopen.domain.repository.AuthRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerProbe
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject

class ProbeServerUseCase @Inject constructor(
    private val auth: AuthRepository,
) {
    suspend operator fun invoke(host: String, port: Int): ApiResult<ServerProbe> =
        auth.probeFingerprint(host, port)
}
