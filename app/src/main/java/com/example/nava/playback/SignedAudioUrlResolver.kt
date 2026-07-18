package com.example.nava.playback

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class SignedAudioUrlResolver @Inject constructor(private val supabase: SupabaseClient) {
    suspend fun resolve(storageUri: String): String {
        require(storageUri.startsWith(PREFIX)) { "Expected an audio Storage URI." }
        return supabase.storage.from(BUCKET).createSignedUrl(storageUri.removePrefix(PREFIX), 10.minutes)
    }

    private companion object { const val BUCKET = "audio"; const val PREFIX = "storage://audio/" }
}
