package com.example.nava.data.downloads

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.nava.domain.catalog.HomeTrack
import com.example.nava.playback.SignedAudioUrlResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun observeDownloads(): Flow<List<OfflineTrackEntity>> = merge(
        dao.observeAll(),
        workManager.getWorkInfosByTagFlow(DOWNLOAD_TAG).map { dao.getAll() },
    )

    fun observeActiveDownloads(): Flow<List<DownloadTransfer>> = workManager
        .getWorkInfosByTagFlow(DOWNLOAD_TAG)
        .map { workInfos ->
            workInfos.filter { it.state in ACTIVE_STATES }.map { work ->
                DownloadTransfer(
                    trackId = work.progress.getString(AudioDownloadWorker.KEY_TRACK_ID)
                        ?: work.tags.firstOrNull { it.startsWith(TRACK_TAG_PREFIX) }?.removePrefix(TRACK_TAG_PREFIX)
                        ?: work.id.toString(),
                    title = work.progress.getString(AudioDownloadWorker.KEY_TITLE)
                        ?: work.outputData.getString(AudioDownloadWorker.KEY_TITLE)
                        ?: work.tags.firstOrNull { it.startsWith(TITLE_TAG_PREFIX) }?.removePrefix(TITLE_TAG_PREFIX).orEmpty(),
                    artistName = work.tags.firstOrNull { it.startsWith(ARTIST_TAG_PREFIX) }?.removePrefix(ARTIST_TAG_PREFIX).orEmpty(),
                    coverImageUrl = work.tags.firstOrNull { it.startsWith(COVER_TAG_PREFIX) }?.removePrefix(COVER_TAG_PREFIX).orEmpty(),
                    progressPercent = work.progress.getInt(AudioDownloadWorker.KEY_PROGRESS_PERCENT, 0).coerceIn(0, 100),
                )
            }
        }

    suspend fun requestDownload(track: HomeTrack): Result<Unit> = runCatching {
        val authorization = supabase.postgrest.rpc("authorize_track_download", buildJsonObject {
            put("p_track_id", track.id)
        }).decodeList<DownloadAuthorizationDto>().single()
        val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setInputData(Data.Builder()
                .putString(AudioDownloadWorker.KEY_TRACK_ID, track.id)
                .putString(AudioDownloadWorker.KEY_SIGNED_URL, resolver.resolve(authorization.audioUrl))
                .putString(AudioDownloadWorker.KEY_TITLE, track.title)
                .putString(AudioDownloadWorker.KEY_ARTIST, track.artistName)
                .putString(AudioDownloadWorker.KEY_COVER, track.coverImageUrl)
                .build())
            .addTag(DOWNLOAD_TAG)
            .addTag("$TRACK_TAG_PREFIX${track.id}")
            .addTag("$TITLE_TAG_PREFIX${track.title}")
            .addTag("$ARTIST_TAG_PREFIX${track.artistName}")
            .addTag("$COVER_TAG_PREFIX${track.coverImageUrl}")
            .build()
        workManager.enqueueUniqueWork("download-${track.id}", ExistingWorkPolicy.KEEP, request)
    }.fold(
        onSuccess = { Result.success(Unit) },
        onFailure = { Result.failure(it.toDownloadFailure()) },
    )

    suspend fun remove(track: OfflineTrackEntity) {
        File(track.audioPath).delete()
        dao.delete(track.trackId)
    }

    suspend fun find(trackId: String): OfflineTrackEntity? = dao.find(trackId)
}

data class DownloadTransfer(
    val trackId: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val progressPercent: Int,
)

private const val DOWNLOAD_TAG = "offline-download"
private const val TRACK_TAG_PREFIX = "download-track:"
private const val TITLE_TAG_PREFIX = "download-title:"
private const val ARTIST_TAG_PREFIX = "download-artist:"
private const val COVER_TAG_PREFIX = "download-cover:"
private val ACTIVE_STATES = setOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED)

@Serializable private data class DownloadAuthorizationDto(
    @SerialName("audio_url") val audioUrl: String,
    @SerialName("expires_in_seconds") val expiresInSeconds: Int,
)
