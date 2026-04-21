package de.kiefer_networks.proxmoxopen.data.api.repository

import de.kiefer_networks.proxmoxopen.data.api.ProxmoxApiClient
import de.kiefer_networks.proxmoxopen.data.api.dto.ClusterResourceDto
import de.kiefer_networks.proxmoxopen.data.api.session.ProxmoxSessionManager
import de.kiefer_networks.proxmoxopen.domain.model.Credentials
import de.kiefer_networks.proxmoxopen.domain.model.GuestStatus
import de.kiefer_networks.proxmoxopen.domain.model.GuestType
import de.kiefer_networks.proxmoxopen.domain.model.Realm
import de.kiefer_networks.proxmoxopen.domain.model.SearchResult
import de.kiefer_networks.proxmoxopen.domain.repository.SearchRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.result.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val sessions: ProxmoxSessionManager,
    private val servers: ServerRepository,
) : SearchRepository {

    override suspend fun search(serverId: Long, query: String): ApiResult<List<SearchResult>> {
        val tokens = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ApiResult.Success(emptyList())

        return call(serverId) { api ->
            api.listClusterResources(type = null)
                .asSequence()
                .filter { tokens.all { token -> it.matches(token) } }
                .mapNotNull { it.toSearchResultOrNull() }
                .toList()
        }
    }

    private fun ClusterResourceDto.matches(token: String): Boolean {
        val lower = token.lowercase()
        // type exact match against "qemu", "lxc", "node", "storage"
        if (type.equals(token, ignoreCase = true)) return true
        // vmid exact or prefix
        vmid?.toString()?.let { if (it == token || it.startsWith(token)) return true }
        // name contains (case-insensitive)
        if (name?.contains(token, ignoreCase = true) == true) return true
        // node contains (case-insensitive)
        if (node?.contains(token, ignoreCase = true) == true) return true
        // storage id contains (case-insensitive)
        if (storage?.contains(token, ignoreCase = true) == true) return true
        // tags contain (proxmox separates tags with ';' or ',')
        tags?.let { raw ->
            val parts = raw.split(';', ',').map { it.trim().lowercase() }
            if (parts.any { it.contains(lower) }) return true
        }
        return false
    }

    private fun ClusterResourceDto.toSearchResultOrNull(): SearchResult? = when (type.lowercase()) {
        "qemu", "lxc" -> {
            val vm = vmid ?: return null
            val nodeName = node ?: return null
            val typeEnum = GuestType.fromApiPath(type.lowercase()) ?: return null
            SearchResult.GuestResult(
                id = id,
                vmid = vm,
                name = name ?: vm.toString(),
                node = nodeName,
                type = typeEnum,
                status = GuestStatus.fromProxmox(status),
                tags = tags?.split(';', ',')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList(),
            )
        }
        "node" -> {
            val nodeName = node ?: return null
            SearchResult.NodeResult(
                id = id,
                node = nodeName,
                status = status,
            )
        }
        "storage" -> {
            val storageName = storage ?: return null
            val nodeName = node ?: return null
            SearchResult.StorageResult(
                id = id,
                storage = storageName,
                node = nodeName,
                content = content,
                status = status,
            )
        }
        else -> null
    }

    private suspend fun <T> call(
        serverId: Long,
        block: suspend (ProxmoxApiClient) -> T,
    ): ApiResult<T> = runCatchingApi {
        val server = servers.getById(serverId)
            ?: error("server $serverId missing")
        val credentials = tokenCredentialsIfNeeded(serverId, server.realm, server.username, server.tokenId)
        block(sessions.apiClient(server, credentials))
    }

    private suspend fun tokenCredentialsIfNeeded(
        serverId: Long,
        realm: Realm,
        username: String?,
        tokenId: String?,
    ): Credentials? {
        if (realm != Realm.PVE_TOKEN) return null
        val secret = servers.getTokenSecret(serverId) ?: return null
        return Credentials.ApiToken(
            username = username ?: "root",
            realm = realm,
            tokenId = tokenId ?: "",
            tokenSecret = secret,
        )
    }
}
