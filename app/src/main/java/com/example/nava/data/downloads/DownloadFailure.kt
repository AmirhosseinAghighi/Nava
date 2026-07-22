package com.example.nava.data.downloads

sealed class DownloadFailure(cause: Throwable) : Exception(cause)

class PremiumDownloadRequired(cause: Throwable) : DownloadFailure(cause)

class DownloadUnavailable(cause: Throwable) : DownloadFailure(cause)

internal fun Throwable.toDownloadFailure(): DownloadFailure {
    val messages = generateSequence(this as Throwable?) { it.cause }
        .mapNotNull(Throwable::message)
        .toList()
    val premiumRequired = messages.any {
        it.contains(PREMIUM_REQUIRED_MESSAGE, ignoreCase = true) ||
            it.contains(PREMIUM_REQUIRED_CODE, ignoreCase = true)
    }
    return if (premiumRequired) PremiumDownloadRequired(this) else DownloadUnavailable(this)
}

private const val PREMIUM_REQUIRED_MESSAGE = "premium subscription required"
private const val PREMIUM_REQUIRED_CODE = "42501"
