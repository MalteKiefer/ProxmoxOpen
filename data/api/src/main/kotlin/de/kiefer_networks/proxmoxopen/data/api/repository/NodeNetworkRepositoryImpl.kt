package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.dto.NodeNetworkIfaceDto
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.NodeNetworkIface
import de.kiefer_networks.proxmoxopen.domain.model.NodeNetworkIfaceType
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.repository.NodeNetworkRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiError
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeNetworkRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : NodeNetworkRepository {

    override suspend fun listInterfaces(
        serverId: Long,
        node: String,
    ): ApiResult<List<NodeNetworkIface>> = runCatchingApi {
        val server = servers.getById(serverId)
            ?: throw IllegalStateException("server $serverId missing")
        val credentials = when (server.realm) {
            Realm.PVE_TOKEN -> {
                val secret = servers.getTokenSecret(serverId)
                    ?: return ApiResult.Failure(ApiError.Auth("token missing"))
                Credentials.ApiToken(
                    username = server.username ?: "root",
                    realm = server.realm,
                    tokenId = server.tokenId ?: "",
                    tokenSecret = secret,
                )
            }
            else -> null
        }
        sessions.apiClient(server, credentials).getNodeNetwork(node).map { it.toDomain() }
    }
}

internal fun NodeNetworkIfaceDto.toDomain(): NodeNetworkIface = NodeNetworkIface(
    iface = iface,
    type = NodeNetworkIfaceType.fromProxmox(type),
    rawType = type,
    active = active?.let { it != 0 },
    autostart = autostart?.let { it != 0 },
    method = method,
    method6 = method6,
    address = address,
    netmask = netmask,
    gateway = gateway,
    address6 = address6,
    netmask6 = netmask6,
    gateway6 = gateway6,
    cidr = cidr,
    cidr6 = cidr6,
    bridgePorts = splitList(bridgePorts),
    bridgeStp = bridgeStp,
    bridgeFd = bridgeFd,
    bondMode = bondMode,
    bondMiimon = bondMiimon,
    slaves = splitList(slaves),
    vlanId = vlanId,
    vlanRawDevice = vlanRawDevice,
    mtu = mtu,
    comments = comments,
)

private fun splitList(raw: String?): List<String> =
    raw?.split(' ', ',', '\t')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
