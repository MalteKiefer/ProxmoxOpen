package de.kiefer_networks.proxmoxopen.data.api

import de.kiefer_networks.proxmoxopen.data.api.dto.ApiResponse
import de.kiefer_networks.proxmoxopen.data.api.dto.AptUpdateDto
import de.kiefer_networks.proxmoxopen.data.api.dto.ClusterResourceDto
import de.kiefer_networks.proxmoxopen.data.api.dto.ClusterStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.ContainerCurrentStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.GuestConfigDto
import de.kiefer_networks.proxmoxopen.data.api.dto.HaGroupDto
import de.kiefer_networks.proxmoxopen.data.api.dto.HaResourceDto
import de.kiefer_networks.proxmoxopen.data.api.dto.HaStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.InterfaceDto
import de.kiefer_networks.proxmoxopen.data.api.dto.SnapshotDto
import de.kiefer_networks.proxmoxopen.data.api.dto.AgentNetworkResult
import de.kiefer_networks.proxmoxopen.data.api.dto.BackupJobDto
import de.kiefer_networks.proxmoxopen.data.api.dto.BackupVolumeDto
import de.kiefer_networks.proxmoxopen.data.api.dto.StorageInfoDto
import de.kiefer_networks.proxmoxopen.data.api.dto.VmConfigDto
import de.kiefer_networks.proxmoxopen.data.api.dto.VmCurrentStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.VncProxyDto
import de.kiefer_networks.proxmoxopen.data.api.dto.GuestStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.NodeListDto
import de.kiefer_networks.proxmoxopen.data.api.dto.NodeStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.RrdPointDto
import de.kiefer_networks.proxmoxopen.data.api.dto.TaskDto
import de.kiefer_networks.proxmoxopen.data.api.dto.TaskLogLineDto
import de.kiefer_networks.proxmoxopen.data.api.dto.TaskStatusDto
import de.kiefer_networks.proxmoxopen.data.api.dto.TicketDto
import timber.log.Timber
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.ContentType

/**
 * Thin wrapper over a [HttpClient] pre-configured for one Proxmox server. Callers supply
 * the base URL (scheme://host:port) and either a ticket+CSRF pair or an API token value.
 *
 * All methods return the parsed DTO. On HTTP failure the caller should catch [ProxmoxHttpException].
 */
class ProxmoxApiClient(
    private val http: HttpClient,
    private val baseUrl: String,
    private val authHeader: Authentication,
) {

    sealed interface Authentication {
        data class Ticket(val ticket: String, val csrfToken: String) : Authentication
        data class ApiToken(val headerValue: String) : Authentication
    }

    // --- Auth --------------------------------------------------------------

    suspend fun createTicket(
        username: String,
        password: String,
        realm: String,
        totp: String? = null,
    ): TicketDto {
        val form = Parameters.build {
            append("username", "$username@$realm")
            append("password", password)
            if (totp != null) append("tfa-challenge", totp)
        }
        val response = http.submitForm(
            url = "$baseUrl/api2/json/access/ticket",
            formParameters = form,
        )
        return response.body<ApiResponse<TicketDto>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty ticket response")
    }

    // --- Cluster & nodes ---------------------------------------------------

    suspend fun getClusterStatus(): List<ClusterStatusDto> =
        http.getJson<List<ClusterStatusDto>>("$baseUrl/api2/json/cluster/status")

    suspend fun getNodes(): List<NodeListDto> =
        http.getJson<List<NodeListDto>>("$baseUrl/api2/json/nodes")

    suspend fun getNodeStatus(node: String): NodeStatusDto =
        http.getJson<NodeStatusDto>("$baseUrl/api2/json/nodes/$node/status")

    suspend fun getNodeRrd(node: String, timeframe: String): List<RrdPointDto> =
        http.getJson<List<RrdPointDto>>("$baseUrl/api2/json/nodes/$node/rrddata?timeframe=$timeframe")

    // --- HA (read-only) ----------------------------------------------------

    suspend fun getHaStatus(): List<HaStatusDto> =
        http.getJson<List<HaStatusDto>>("$baseUrl/api2/json/cluster/ha/status/current")

    suspend fun getHaResources(): List<HaResourceDto> =
        http.getJson<List<HaResourceDto>>("$baseUrl/api2/json/cluster/ha/resources")

    suspend fun getHaGroups(): List<HaGroupDto> =
        http.getJson<List<HaGroupDto>>("$baseUrl/api2/json/cluster/ha/groups")

    // --- Guests ------------------------------------------------------------

    suspend fun listClusterResources(type: String? = "vm"): List<ClusterResourceDto> {
        val url = if (type.isNullOrBlank()) {
            "$baseUrl/api2/json/cluster/resources"
        } else {
            "$baseUrl/api2/json/cluster/resources?type=$type"
        }
        return http.getJson<List<ClusterResourceDto>>(url)
    }

    suspend fun getGuestStatus(node: String, type: String, vmid: Int): GuestStatusDto =
        http.getJson<GuestStatusDto>("$baseUrl/api2/json/nodes/$node/$type/$vmid/status/current")

    suspend fun getGuestRrd(
        node: String,
        type: String,
        vmid: Int,
        timeframe: String,
    ): List<RrdPointDto> = http.getJson<List<RrdPointDto>>(
        "$baseUrl/api2/json/nodes/$node/$type/$vmid/rrddata?timeframe=$timeframe",
    )

    // --- Config --------------------------------------------------------------

    suspend fun getGuestConfig(node: String, type: String, vmid: Int): GuestConfigDto =
        http.getJson<GuestConfigDto>("$baseUrl/api2/json/nodes/$node/$type/$vmid/config")

    suspend fun setGuestConfig(
        node: String,
        type: String,
        vmid: Int,
        params: Map<String, String>,
    ): String? {
        val form = Parameters.build { params.forEach { (k, v) -> append(k, v) } }
        val response = http.submitForm(
            url = "$baseUrl/api2/json/nodes/$node/$type/$vmid/config",
            formParameters = form,
        ) { applyAuth(); method = io.ktor.http.HttpMethod.Put }
        return response.body<ApiResponse<String?>>().data
    }

    // --- Extended CT status --------------------------------------------------

    suspend fun getContainerStatus(node: String, vmid: Int): ContainerCurrentStatusDto =
        http.getJson<ContainerCurrentStatusDto>(
            "$baseUrl/api2/json/nodes/$node/lxc/$vmid/status/current",
        )

    suspend fun getContainerInterfaces(node: String, vmid: Int): List<InterfaceDto> =
        try {
            http.getJson<List<InterfaceDto>>(
                "$baseUrl/api2/json/nodes/$node/lxc/$vmid/interfaces",
            )
        } catch (e: Exception) {
            Timber.d(e, "Failed to load container interfaces")
            emptyList()
        }

    // --- Snapshots -----------------------------------------------------------

    suspend fun listSnapshots(node: String, type: String, vmid: Int): List<SnapshotDto> =
        http.getJson<List<SnapshotDto>>(
            "$baseUrl/api2/json/nodes/$node/$type/$vmid/snapshot",
        )

    suspend fun createSnapshot(
        node: String, type: String, vmid: Int, snapname: String, description: String?,
    ): String {
        val form = Parameters.build {
            append("snapname", snapname)
            description?.let { append("description", it) }
        }
        val response = http.submitForm(
            url = "$baseUrl/api2/json/nodes/$node/$type/$vmid/snapshot",
            formParameters = form,
        ) { applyAuth() }
        return response.body<ApiResponse<String>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty UPID")
    }

    suspend fun rollbackSnapshot(
        node: String, type: String, vmid: Int, snapname: String,
    ): String {
        val response = http.post(
            "$baseUrl/api2/json/nodes/$node/$type/$vmid/snapshot/$snapname/rollback",
        ) { applyAuth() }
        return response.body<ApiResponse<String>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty UPID")
    }

    suspend fun deleteSnapshot(
        node: String, type: String, vmid: Int, snapname: String,
    ): String? {
        val response = http.delete(
            "$baseUrl/api2/json/nodes/$node/$type/$vmid/snapshot/$snapname",
        ) { applyAuth() }
        return response.body<ApiResponse<String?>>().data
    }

    // --- Cluster Backup Jobs ---------------------------------------------------

    suspend fun listBackupJobs(): List<BackupJobDto> =
        http.getJson<List<BackupJobDto>>("$baseUrl/api2/json/cluster/backup")

    suspend fun runBackupJob(id: String): String {
        val response = http.post("$baseUrl/api2/json/cluster/backup/$id") { applyAuth() }
        return response.body<ApiResponse<String>>().data ?: "started"
    }

    suspend fun createBackupJob(params: Map<String, String>): String? {
        val form = Parameters.build { params.forEach { (k, v) -> append(k, v) } }
        val response = http.submitForm("$baseUrl/api2/json/cluster/backup", formParameters = form) { applyAuth() }
        return response.body<ApiResponse<String?>>().data
    }

    suspend fun updateBackupJob(id: String, params: Map<String, String>): String? {
        val form = Parameters.build { params.forEach { (k, v) -> append(k, v) } }
        val response = http.submitForm("$baseUrl/api2/json/cluster/backup/$id", formParameters = form) { applyAuth() }
        return response.body<ApiResponse<String?>>().data
    }

    suspend fun deleteBackupJob(id: String) {
        http.delete("$baseUrl/api2/json/cluster/backup/$id") { applyAuth() }
    }

    // --- Backup (vzdump) -----------------------------------------------------

    suspend fun createBackup(
        node: String, vmid: Int, storage: String?, mode: String, compress: String?,
        protected: Boolean = false, notesTemplate: String? = null,
    ): String {
        val form = Parameters.build {
            append("vmid", vmid.toString())
            append("mode", mode)
            storage?.let { append("storage", it) }
            compress?.let { append("compress", it) }
            if (protected) append("protected", "1")
            notesTemplate?.let { append("notes-template", it) }
        }
        val response = http.submitForm(
            url = "$baseUrl/api2/json/nodes/$node/vzdump",
            formParameters = form,
        ) { applyAuth() }
        return response.body<ApiResponse<String>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty UPID")
    }

    /** List all storages on a node. */
    suspend fun listStorages(node: String): List<StorageInfoDto> =
        try {
            http.getJson<List<StorageInfoDto>>("$baseUrl/api2/json/nodes/$node/storage")
        } catch (e: Exception) { Timber.d(e, "Failed to load storages"); emptyList() }

    /** List storages that support backup content. */
    suspend fun listBackupStorages(node: String): List<StorageInfoDto> =
        try {
            http.getJson<List<StorageInfoDto>>("$baseUrl/api2/json/nodes/$node/storage?content=backup")
        } catch (e: Exception) { Timber.d(e, "Failed to load backup storages"); emptyList() }

    /** List backup volumes for a specific vmid on a given storage. */
    suspend fun listBackups(node: String, storage: String, vmid: Int): List<BackupVolumeDto> =
        try {
            http.getJson<List<BackupVolumeDto>>(
                "$baseUrl/api2/json/nodes/$node/storage/$storage/content?content=backup&vmid=$vmid",
            )
        } catch (e: Exception) { Timber.d(e, "Failed to load backups"); emptyList() }

    /** Restore a backup (vzdump archive) to a container. */
    suspend fun restoreBackup(node: String, vmid: Int, archive: String, storage: String?): String {
        val form = Parameters.build {
            append("vmid", vmid.toString())
            append("ostemplate", archive)
            append("restore", "1")
            append("force", "1")
            storage?.let { append("storage", it) }
        }
        val response = http.submitForm(
            url = "$baseUrl/api2/json/nodes/$node/lxc",
            formParameters = form,
        ) { applyAuth() }
        return response.body<ApiResponse<String>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty UPID")
    }

    // --- Node power actions --------------------------------------------------

    suspend fun nodeAction(node: String, command: String): String? {
        val form = Parameters.build { append("command", command) }
        val response = http.submitForm(
            url = "$baseUrl/api2/json/nodes/$node/status",
            formParameters = form,
        ) { applyAuth() }
        return response.body<ApiResponse<String?>>().data
    }

    // --- Terminal proxy (nodes + LXC) ----------------------------------------

    /** Shell console for a node. */
    suspend fun createNodeTermProxy(node: String): de.kiefer_networks.proxmoxopen.data.api.dto.VncProxyDto {
        val response = http.post("$baseUrl/api2/json/nodes/$node/termproxy") { applyAuth() }
        return response.body<ApiResponse<de.kiefer_networks.proxmoxopen.data.api.dto.VncProxyDto>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty termproxy response")
    }

    /** Terminal for an LXC container. */
    suspend fun createLxcTermProxy(node: String, vmid: Int): de.kiefer_networks.proxmoxopen.data.api.dto.VncProxyDto {
        val response = http.post("$baseUrl/api2/json/nodes/$node/lxc/$vmid/termproxy") { applyAuth() }
        return response.body<ApiResponse<de.kiefer_networks.proxmoxopen.data.api.dto.VncProxyDto>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty termproxy response")
    }

    // --- QEMU VM specific ----------------------------------------------------

    suspend fun getVmStatus(node: String, vmid: Int): VmCurrentStatusDto =
        http.getJson<VmCurrentStatusDto>("$baseUrl/api2/json/nodes/$node/qemu/$vmid/status/current")

    suspend fun getVmConfig(node: String, vmid: Int): VmConfigDto =
        http.getJson<VmConfigDto>("$baseUrl/api2/json/nodes/$node/qemu/$vmid/config")

    suspend fun setVmConfig(node: String, vmid: Int, params: Map<String, String>): String? {
        val form = Parameters.build { params.forEach { (k, v) -> append(k, v) } }
        val response = http.submitForm(
            url = "$baseUrl/api2/json/nodes/$node/qemu/$vmid/config",
            formParameters = form,
        ) { applyAuth(); method = io.ktor.http.HttpMethod.Put }
        return response.body<ApiResponse<String?>>().data
    }

    suspend fun getVmAgentInterfaces(node: String, vmid: Int): List<de.kiefer_networks.proxmoxopen.data.api.dto.AgentInterfaceDto> {
        return try {
            // Short timeout — agent can be slow or absent
            val resp = kotlinx.coroutines.withTimeout(5_000) {
                http.get("$baseUrl/api2/json/nodes/$node/qemu/$vmid/agent/network-get-interfaces") { applyAuth() }
            }
            if (!resp.status.isSuccess()) return emptyList()
            val result = resp.body<ApiResponse<AgentNetworkResult>>().data
            result?.result ?: emptyList()
        } catch (e: Exception) { Timber.d(e, "Failed to load agent interfaces"); emptyList() }
    }

    // --- Power actions -----------------------------------------------------

    /**
     * Executes a power action. Returns the UPID of the triggered task.
     * [action] must be one of: start, stop, shutdown, reboot, suspend, resume, reset.
     */
    suspend fun powerAction(node: String, type: String, vmid: Int, action: String): String {
        val url = "$baseUrl/api2/json/nodes/$node/$type/$vmid/status/$action"
        val response = http.post(url) {
            applyAuth()
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        val body = response.body<ApiResponse<String>>()
        return body.data ?: throw ProxmoxHttpException(response.status.value, "empty UPID response")
    }

    // --- Tasks -------------------------------------------------------------

    suspend fun listTasks(node: String, limit: Int = 50, vmid: Int? = null): List<TaskDto> {
        val url = buildString {
            append("$baseUrl/api2/json/nodes/$node/tasks?limit=$limit")
            vmid?.let { append("&vmid=$it") }
        }
        return http.getJson<List<TaskDto>>(url)
    }

    suspend fun getTaskStatus(node: String, upid: String): TaskStatusDto {
        val encoded = URLBuilder().apply { parameters.append("upid", upid) }.parameters["upid"] ?: upid
        return http.getJson<TaskStatusDto>("$baseUrl/api2/json/nodes/$node/tasks/$encoded/status")
    }

    suspend fun getTaskLog(
        node: String,
        upid: String,
        start: Int = 0,
        limit: Int = 500,
    ): List<TaskLogLineDto> {
        val encoded = URLBuilder().apply { parameters.append("upid", upid) }.parameters["upid"] ?: upid
        return http.getJson<List<TaskLogLineDto>>(
            "$baseUrl/api2/json/nodes/$node/tasks/$encoded/log?start=$start&limit=$limit",
        )
    }

    // --- Delete guest ------------------------------------------------------

    /**
     * Deletes a VM or container. Returns the UPID of the triggered task.
     */
    suspend fun deleteGuest(
        node: String,
        type: String,
        vmid: Int,
        purge: Boolean = true,
        destroyUnreferencedDisks: Boolean = true,
    ): String {
        val params = buildString {
            val parts = mutableListOf<String>()
            if (purge) parts += "purge=1"
            if (destroyUnreferencedDisks) parts += "destroy-unreferenced-disks=1"
            if (parts.isNotEmpty()) append("?${parts.joinToString("&")}")
        }
        val response = http.delete(
            "$baseUrl/api2/json/nodes/$node/$type/$vmid$params",
        ) { applyAuth() }
        return response.body<ApiResponse<String>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty UPID")
    }

    // --- Migration ---------------------------------------------------------

    suspend fun migrateGuest(
        node: String,
        type: String,
        vmid: Int,
        target: String,
        online: Boolean = false,
        withLocalDisks: Boolean = false,
        targetStorage: String? = null,
    ): String {
        val form = Parameters.build {
            append("target", target)
            if (online) append("online", "1")
            if (type == "qemu" && withLocalDisks) append("with-local-disks", "1")
            targetStorage?.let { append("targetstorage", it) }
        }
        val response = http.submitForm(
            url = "$baseUrl/api2/json/nodes/$node/$type/$vmid/migrate",
            formParameters = form,
        ) { applyAuth() }
        return response.body<ApiResponse<String>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty UPID")
    }

    // --- Clone ---------------------------------------------------------------

    suspend fun cloneGuest(
        node: String,
        type: String,
        vmid: Int,
        newid: Int,
        name: String? = null,
        full: Boolean = true,
        target: String? = null,
        storage: String? = null,
    ): String {
        val form = Parameters.build {
            append("newid", newid.toString())
            name?.let { append("name", it) }
            append("full", if (full) "1" else "0")
            target?.let { append("target", it) }
            storage?.let { append("storage", it) }
        }
        val response = http.submitForm(
            url = "$baseUrl/api2/json/nodes/$node/$type/$vmid/clone",
            formParameters = form,
        ) { applyAuth() }
        return response.body<ApiResponse<String>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty UPID")
    }

    // --- APT updates --------------------------------------------------------

    suspend fun listAptUpdates(node: String): List<AptUpdateDto> =
        http.getJson<List<AptUpdateDto>>("$baseUrl/api2/json/nodes/$node/apt/update")

    /** Refresh the package database. Returns the UPID of the triggered task. */
    suspend fun refreshApt(node: String): String {
        val response = http.post("$baseUrl/api2/json/nodes/$node/apt/update") { applyAuth() }
        return response.body<ApiResponse<String>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty UPID")
    }

    /** Start a dist-upgrade of all pending packages. Returns the UPID of the triggered task. */
    suspend fun upgradeApt(node: String): String {
        val response = http.post("$baseUrl/api2/json/nodes/$node/apt/upgrade") { applyAuth() }
        return response.body<ApiResponse<String>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty UPID")
    }

    // --- Console (VNC proxy) -----------------------------------------------

    suspend fun createVncProxy(node: String, type: String, vmid: Int): VncProxyDto {
        val form = io.ktor.http.Parameters.build { append("websocket", "1") }
        val response = http.submitForm(
            "$baseUrl/api2/json/nodes/$node/$type/$vmid/vncproxy",
            formParameters = form,
        ) { applyAuth() }
        return response.body<ApiResponse<VncProxyDto>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty vncproxy response")
    }

    // --- Helpers -----------------------------------------------------------

    private suspend inline fun <reified T> HttpClient.getJson(url: String): T {
        val response = get(url) { applyAuth() }
        if (!response.status.isSuccess()) {
            throw ProxmoxHttpException(response.status.value, "GET $url -> ${response.status}")
        }
        return response.body<ApiResponse<T>>().data
            ?: throw ProxmoxHttpException(response.status.value, "empty response body")
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuth() {
        when (val a = authHeader) {
            is Authentication.ApiToken -> headers { append(HttpHeaders.Authorization, a.headerValue) }
            is Authentication.Ticket -> headers {
                append("Cookie", "PVEAuthCookie=${a.ticket}")
                append("CSRFPreventionToken", a.csrfToken)
            }
        }
    }
}

private fun io.ktor.http.HttpStatusCode.isSuccess(): Boolean = value in 200..299

class ProxmoxHttpException(val code: Int, message: String) : RuntimeException(message)
