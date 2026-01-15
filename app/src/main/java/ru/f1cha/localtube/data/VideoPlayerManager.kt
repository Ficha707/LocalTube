package ru.f1cha.localtube.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ru.f1cha.localtube.service.VideoPlayerService

class VideoPlayerManager(private val context: Context, private val stateManager: VideoStateManager) {

    private var player: ExoPlayer? = null
    private var currentVideo: Video? = null

    fun preparePlayerWithoutAutoPlay(playerView: PlayerView, video: Video) {
        // Сохраняем прогресс текущего видео перед переключением
        currentVideo?.let { saveCurrentPosition(it.id) }

        currentVideo = video

        // Привязываем плеер к PlayerView
        playerView.player = player
        playerView.visibility = android.view.View.VISIBLE

        // Создаем MediaItem из видео
        val mediaItem = MediaItem.fromUri(video.getUri())

        // Устанавливаем видео в плеер
        player?.setMediaItem(mediaItem)
        player?.prepare()

        // Восстанавливаем сохраненную позицию
        val savedPosition = stateManager.getProgress(video.id)
        if (savedPosition > 0) {
            player?.seekTo(savedPosition)
        }

        // НЕ начинаем воспроизведение автоматически
        // player?.play() // Закомментировано

        // НЕ запускаем сервис, так как видео не воспроизводится
        // startBackgroundService() // Закомментировано
    }

    // Инициализируем плеер
    fun initialize(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
            // Настраиваем воспроизведение в фоне
            configureBackgroundPlayback()
        }
        return player!!
    }

    private fun configureBackgroundPlayback() {
        player?.let { player ->
            // Разрешаем воспроизведение в фоне
            player.setHandleAudioBecomingNoisy(true)

            // Настраиваем слушатель для управления воспроизведением
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING, Player.STATE_READY -> {
                            // Запускаем сервис при начале воспроизведения
                            startBackgroundService()
                        }
                        Player.STATE_ENDED, Player.STATE_IDLE -> {
                            // Останавливаем сервис при завершении
                            stopBackgroundService()
                        }
                    }
                }
            })
        }
    }

    private fun startBackgroundService() {
        VideoPlayerService.startService(context)
    }

    private fun stopBackgroundService() {
        VideoPlayerService.stopService(context)
    }

    // Подготавливаем плеер для просмотра видео
    fun preparePlayer(playerView: PlayerView, video: Video) {
        // Сохраняем прогресс текущего видео перед переключением
        currentVideo?.let { saveCurrentPosition(it.id) }

        currentVideo = video

        // Привязываем плеер к PlayerView
        playerView.player = player
        playerView.visibility = android.view.View.VISIBLE

        // Создаем MediaItem из видео
        val mediaItem = MediaItem.fromUri(video.getUri())

        // Устанавливаем видео в плеер
        player?.setMediaItem(mediaItem)
        player?.prepare()

        // Восстанавливаем сохраненную позицию
        val savedPosition = stateManager.getProgress(video.id)
        if (savedPosition > 0) {
            player?.seekTo(savedPosition)
        }

        // Начинаем воспроизведение
        player?.play()

        // Запускаем сервис для фонового воспроизведения
        startBackgroundService()
    }

    // Получаем текущую позицию просмотра
    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0
    }

    // Получаем текущее видео
    fun getCurrentVideo(): Video? {
        return currentVideo
    }

    // Приостанавливаем воспроизведение
    fun pause() {
        player?.pause()
        // Не останавливаем сервис при паузе, только при полной остановке
    }

    // Возобновляем воспроизведение
    fun play() {
        player?.play()
        // Если сервис не запущен, запускаем
        if (player?.playbackState == Player.STATE_READY ||
            player?.playbackState == Player.STATE_BUFFERING) {
            startBackgroundService()
        }
    }

    // Освобождаем ресурсы плеера
    fun release() {
        // Сохраняем позицию текущего видео
        currentVideo?.let { video ->
            saveCurrentPosition(video.id)
        }

        // Останавливаем сервис
        stopBackgroundService()

        player?.release()
        player = null
        currentVideo = null
    }

    // Сохраняем текущую позицию просмотра
    fun saveCurrentPosition(videoId: Long) {
        val position = getCurrentPosition()
        stateManager.saveProgress(videoId, position)
        stateManager.saveLastVideo(videoId, position)
    }

    // Получаем ID последнего просмотренного видео
    fun getLastVideoId(): Long {
        return stateManager.getLastVideoId()
    }

    // Перематываем на определенную позицию
    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    // Проверяем, играет ли видео
    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    // Получаем длительность текущего видео
    fun getDuration(): Long {
        return player?.duration ?: 0
    }
}