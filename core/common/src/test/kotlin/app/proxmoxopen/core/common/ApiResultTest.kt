package app.proxmoxopen.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApiResultTest {
    @Test
    fun `map transforms success value`() {
        val r: ApiResult<Int> = ApiResult.Success(2)
        val m = r.map { it * 3 }
        assertTrue(m is ApiResult.Success)
        assertEquals(6, (m as ApiResult.Success).value)
    }

    @Test
    fun `map leaves failure unchanged`() {
        val r: ApiResult<Int> = ApiResult.Failure(ApiError.Network("x"))
        val m = r.map { it * 3 }
        assertTrue(m is ApiResult.Failure)
    }

    @Test
    fun `onSuccess runs on success only`() {
        var n = 0
        ApiResult.Success(5).onSuccess { n = it }
        assertEquals(5, n)

        val failed: ApiResult<Int> = ApiResult.Failure(ApiError.Auth("nope"))
        failed.onSuccess { n = 99 }
        assertEquals(5, n)
    }
}
