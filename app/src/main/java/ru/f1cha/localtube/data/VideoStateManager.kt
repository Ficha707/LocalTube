package ru.f1cha.localtube.data

import android.content.Context
import android.content.SharedPreferences

class VideoStateManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "video_state"
        private const val KEY_PREFIX_PROGRESS = "progress_"
        private const val KEY_PREFIX_WATCHED = "watched_"
        private const val KEY_LAST_VIDEO_ID = "last_video_id"
        private const val KEY_LAST_VIDEO_POSITION = "last_video_position"
        private const val KEY_PINNED_VIDEOS = "pinned_videos"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // === Прогресс просмотра ===
    fun saveProgress(videoId: Long, position: Long) {
        prefs.edit().putLong("${KEY_PREFIX_PROGRESS}$videoId", position).apply()
    }

    fun getProgress(videoId: Long): Long {
        return prefs.getLong("${KEY_PREFIX_PROGRESS}$videoId", 0L)
    }

    fun getProgressPercentage(videoId: Long, duration: Long): Int {
        if (duration == 0L) return 0
        val progress = getProgress(videoId)
        return ((progress * 100) / duration).toInt()
    }

    // === Последнее видео ===
    fun saveLastVideo(videoId: Long, position: Long) {
        prefs.edit().apply {
            putLong(KEY_LAST_VIDEO_ID, videoId)
            putLong(KEY_LAST_VIDEO_POSITION, position)
            apply()
        }
    }

    fun getLastVideoId(): Long = prefs.getLong(KEY_LAST_VIDEO_ID, 0L)
    fun getLastVideoPosition(): Long = prefs.getLong(KEY_LAST_VIDEO_POSITION, 0L)
}