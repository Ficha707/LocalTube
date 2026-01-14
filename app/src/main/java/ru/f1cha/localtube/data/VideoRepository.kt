package ru.f1cha.localtube.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

class VideoRepository(private val context: Context) {

    companion object {
        private const val TAG = "VideoRepository"
        private const val MOVIES_FOLDER_NAME = "Movies"
    }

    // Получаем путь к стандартной папке Movies
    private fun getMoviesFolderPath(): String {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        return moviesDir.absolutePath
    }

    fun deleteVideo(video: Video): Boolean {
        return try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentUris.withAppendedId(
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    video.id
                )
            } else {
                ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    video.id
                )
            }

            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            if (rowsDeleted > 0) {
                Log.d(TAG, "Видео удалено через MediaStore: ${video.title}")
                true
            } else {
                Log.e(TAG, "Не удалось удалить видео через MediaStore: ${video.title}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении видео: ${e.message}")
            false
        }
    }

    fun getAllVideos(): List<Video> {
        val videos = mutableListOf<Video>()
        val moviesFolderPath = getMoviesFolderPath()

        Log.d(TAG, "Сканируем папку Movies: $moviesFolderPath")

        // Проекция - какие поля нам нужны
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        // Сортировка по дате добавления (новые сверху)
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        // Условие WHERE: фильтруем только файлы из папки Movies
        val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$moviesFolderPath/%")

        val cursor: Cursor? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Для Android 10 и выше
                context.contentResolver.query(
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )
            } else {
                // Для Android 9 и ниже
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешения на чтение файлов: ${e.message}")
            return emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при чтении видео: ${e.message}")
            return emptyList()
        }

        cursor?.use { cursor ->
            // Получаем индексы колонок
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val folderColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            Log.d(TAG, "Найдено видеофайлов в папке Movies: ${cursor.count}")

            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: ""
                    val displayName = cursor.getString(nameColumn) ?: ""
                    val path = cursor.getString(dataColumn) ?: continue
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val folderName = cursor.getString(folderColumn) ?: MOVIES_FOLDER_NAME

                    // Проверяем, что файл существует
                    if (!File(path).exists()) {
                        continue
                    }

                    // Если длительность не указана, пропускаем
                    if (duration <= 0) {
                        continue
                    }

                    // Если название пустое, используем имя файла
                    val finalTitle = if (title.isNotBlank()) title else {
                        File(path).nameWithoutExtension.replace("_", " ")
                    }

                    val video = Video(
                        id = id,
                        title = finalTitle,
                        path = path,
                        duration = duration,
                        size = size,
                        dateAdded = dateAdded * 1000,
                        folderName = folderName,
                        lastPlayedPosition = 0
                    )

                    videos.add(video)

                    Log.d(TAG, "Добавлено видео: $finalTitle, путь: $path")

                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при обработке видео: ${e.message}")
                }
            }
        }

        cursor?.close()

        Log.d(TAG, "Всего загружено видео из папки Movies: ${videos.size}")
        return videos
    }

    fun getVideosByFolder(folderName: String): List<Video> {
        val allVideos = getAllVideos()
        return allVideos.filter { it.folderName == folderName }
    }

    fun getAllFolders(): List<String> {
        val allVideos = getAllVideos()
        return allVideos
            .mapNotNull { it.folderName }
            .distinct()
            .sorted()
    }

    // Получаем миниатюру для видео
    fun getVideoThumbnail(videoId: Long): android.graphics.Bitmap? {
        return try {
            val thumb = MediaStore.Video.Thumbnails.getThumbnail(
                context.contentResolver,
                videoId,
                MediaStore.Video.Thumbnails.MINI_KIND,
                null
            )
            thumb
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось получить превью для видео $videoId: ${e.message}")
            null
        }
    }

    // Получаем путь к превью (для Glide)
    fun getVideoThumbnailUri(videoId: Long): android.net.Uri {
        return android.content.ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoId
        )
    }

    // Получить URI для видео
    fun getVideoUri(video: Video): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentUris.withAppendedId(
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                video.id
            )
        } else {
            Uri.fromFile(File(video.path))
        }
    }
}