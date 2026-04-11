package app.proxmoxopen.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(val data: T? = null)
