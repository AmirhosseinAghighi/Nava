package com.example.nava.data.downloads

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.nava.domain.catalog.HomeTrack
import com.example.nava.playback.SignedAudioUrlResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
    private val resolver: SignedAudioUrlResolver,
    private val dao: OfflineTrackDao,
) {
    fun observeDownloads(): Flow<List<OfflineTrackEntity>> = dao.observeAll()

    suspend fun requestDownload(track: HomeTrack): Result<Unit> = runCatching {
        val authorization = supabase.postgrest.rpc("authorize_track_download", buildJsonObject {
            put("p_track_id", track.id)
        }).decodeSingle<DownloadAuthorizationDto>()
        val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setInputData(Data.Builder()
                .putString(AudioDownloadWorker.KEY_TRACK_ID, track.id)
                .putString(AudioDownloadWorker.KEY_SIGNED_URL, resolver.resolve(authorization.audioUrl))
                .putString(AudioDownloadWorker.KEY_TITLE, track.title)
                .putString(AudioDownloadWorker.KEY_ARTIST, track.artistName)
                .putString(AudioDownloadWorker.KEY_COVER, track.coverImageUrl)
                .build())
            .addTag("offline-download")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("download-${track.id}", ExistingWorkPolicy.KEEP, request)
    }

    suspend fun remove(track: OfflineTrackEntity) {
        File(track.audioPath).delete()
        dao.delete(track.trackId)
    }

    suspend fun find(trackId: String): OfflineTrackEntity? = dao.find(trackId)
}

@Serializable private data class DownloadAuthorizationDto(
    @SerialName("audio_url") val audioUrl: String,
    @SerialName("expires_in_seconds") val expiresInSeconds: Int,
)
