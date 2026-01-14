package ru.f1cha.localtube.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ru.f1cha.localtube.MainActivity
import ru.f1cha.localtube.R
import android.widget.RemoteViews

class VideoPlayerService : Service() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "video_player_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "ru.f1cha.localtube.ACTION_PLAY_PAUSE"

        // Запуск сервиса
        fun startService(context: Context) {
            val intent = Intent(context, VideoPlayerService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        // Остановка сервиса
        fun stopService(context: Context) {
            val intent = Intent(context, VideoPlayerService::class.java)
            context.stopService(intent)
        }
    }

    private var isPlaying = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Обрабатываем действия из уведомления
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                isPlaying = !isPlaying
                // Отправляем broadcast для MainActivity
                val broadcastIntent = Intent(ACTION_PLAY_PAUSE)
                sendBroadcast(broadcastIntent)
            }
        }

        // Создаем уведомление
        val notification = createNotification()

        // Запускаем сервис в foreground режиме
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Воспроизведение видео",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление для воспроизведения видео в фоне"
                setShowBadge(false)
                setSound(null, null) // Без звука
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Создаем custom layout для уведомления
        val notificationLayout = RemoteViews(packageName, R.layout.notification_player)

        // Настраиваем кнопку play/pause
        val playPauseIntent = Intent(this, VideoPlayerService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Устанавливаем иконку play/pause
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        notificationLayout.setImageViewResource(R.id.btnPlayPause, playPauseIcon)

        // Назначаем клик
        notificationLayout.setOnClickPendingIntent(R.id.btnPlayPause, playPausePendingIntent)

        // Intent для возврата в приложение
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setCustomContentView(notificationLayout)
            .setSmallIcon(R.drawable.ic_video_placeholder)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true) // Без звука уведомления
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }
}