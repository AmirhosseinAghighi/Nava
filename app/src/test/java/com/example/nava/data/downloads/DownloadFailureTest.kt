package com.example.nava.data.downloads

import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFailureTest {
    @Test
    fun premiumServerErrorMapsToPremiumFailure() {
        val serverError = IllegalStateException(
            "premium subscription required Code: 42501 URL: /rest/v1/rpc/authorize_track_download",
        )

        assertTrue(serverError.toDownloadFailure() is PremiumDownloadRequired)
    }

    @Test
    fun unrelatedErrorMapsToGenericFailure() {
        val networkError = IllegalStateException("Connection timed out")

        assertTrue(networkError.toDownloadFailure() is DownloadUnavailable)
    }
}
