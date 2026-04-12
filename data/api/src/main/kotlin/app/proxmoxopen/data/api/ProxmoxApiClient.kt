package app.proxmoxopen.data.api

import app.proxmoxopen.data.api.dto.ApiResponse
import app.proxmoxopen.data.api.dto.ClusterResourceDto
import app.proxmoxopen.data.api.dto.ClusterStatusDto
import app.proxmoxopen.data.api.dto.GuestConfigDto
import app.proxmoxopen.data.api.dto.GuestStatusDto
import app.proxmoxopen.data.api.dto.NodeListDto
import app.proxmoxopen.data.api.dto.NodeStatusDto
import app.proxmoxopen.data.api.dto.RrdPointDto
import app.proxmoxopen.data.api.dto.TaskDto
import app.proxmoxopen.data.api.dto.TaskStatusDto
import app.proxmoxopen.data.api.dto.TicketDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
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

    // --- Guests ------------------------------------------------------------

    suspend fun listClusterResources(): List<ClusterResourceDto> =
        http.getJson<List<ClusterResourceDto>>("$baseUrl/api2/json/cluster/resources?type=vm")

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

    suspend fun listTasks(node: String, limit: Int = 50): List<TaskDto> =
        http.getJson<List<TaskDto>>("$baseUrl/api2/json/nodes/$node/tasks?limit=$limit")

    suspend fun getTaskStatus(node: String, upid: String): TaskStatusDto {
        val encoded = URLBuilder().apply { parameters.append("upid", upid) }.parameters["upid"] ?: upid
        return http.getJson<TaskStatusDto>("$baseUrl/api2/json/nodes/$node/tasks/$encoded/status")
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
