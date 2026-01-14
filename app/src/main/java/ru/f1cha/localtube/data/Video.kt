package ru.f1cha.localtube.data

import android.net.Uri
import java.io.File

data class Video(
    val id: Long,
    val title: String,
    val path: String,
    val duration: Long, // в миллисекундах
    val size: Long, // в байтах
    val dateAdded: Long, // timestamp
    val thumbnailPath: String? = null,
    val lastPlayedPosition: Long = 0, // прогресс просмотра
    val folderName: String? = null // для плейлистов
) {
    // Получаем URI для ExoPlayer
    fun getUri(): android.net.Uri {
        return android.net.Uri.fromFile(java.io.File(path))
    }

    // Получаем имя файла
    fun getFileName(): String {
        return File(path).name
    }

    // Получаем имя файла без расширения
    fun getFileNameWithoutExtension(): String {
        return File(path).nameWithoutExtension
    }

    // Форматируем длительность в ЧЧ:ММ:СС
    fun getFormattedDuration(): String {
        val seconds = duration / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    // Форматируем размер файла
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    // Прогресс в процентах
    fun getProgressPercentage(): Int {
        if (duration == 0L) return 0
        return ((lastPlayedPosition * 100) / duration).toInt()
    }

    // Получаем папку, в которой лежит видео
    fun getContainingFolder(): String {
        return File(path).parentFile?.name ?: "Без папки"
    }
}