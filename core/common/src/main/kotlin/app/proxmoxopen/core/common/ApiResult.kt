package app.proxmoxopen.core.common

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}

inline fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.value

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(value)
    return this
}

inline fun <T> ApiResult<T>.onFailure(block: (ApiError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) block(error)
    return this
}

sealed class ApiError(open val message: String) {
    data class Network(override val message: String) : ApiError(message)
    data class Http(val code: Int, override val message: String) : ApiError(message)
    data class Auth(override val message: String) : ApiError(message)
    data class Tls(override val message: String, val fingerprintSha256: String? = null) : ApiError(message)
    data class FingerprintMismatch(val expected: String, val actual: String) :
        ApiError("Server fingerprint changed")
    data class Parse(override val message: String) : ApiError(message)
    data class Unknown(override val message: String) : ApiError(message)
}
