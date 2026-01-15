package ru.f1cha.localtube

import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.f1cha.localtube.adapter.VideoAdapter
import ru.f1cha.localtube.data.VideoPlayerManager
import ru.f1cha.localtube.data.VideoRepository
import ru.f1cha.localtube.service.VideoPlayerService
import ru.f1cha.localtube.data.VideoStateManager
import android.widget.TextView
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import ru.f1cha.localtube.adapter.SwipeToDeleteCallback
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.widget.Toast
import android.widget.ImageButton
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.core.view.isVisible
import androidx.appcompat.app.AlertDialog
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
        private const val REQUEST_MANAGE_EXTERNAL_STORAGE = 101
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvPlayerTitle: TextView
    private lateinit var adapter: VideoAdapter
    private lateinit var repository: VideoRepository
    private lateinit var videoPlayerManager: VideoPlayerManager
    private lateinit var stateManager: VideoStateManager
    private lateinit var loadingView: View
    private lateinit var swipeToDeleteCallback: SwipeToDeleteCallback

    // View элементы
    private lateinit var playerView: androidx.media3.ui.PlayerView
    private lateinit var layoutQueueHeader: android.widget.LinearLayout
    private lateinit var btnShuffle: ImageButton
    private lateinit var btnFullscreen: ImageButton

    // Текущий список видео
    private var allVideos = listOf<ru.f1cha.localtube.data.Video>()
    private var currentPlayingVideo: ru.f1cha.localtube.data.Video? = null
    private var isActivityVisible = true
    private var isFullscreen = false
    private var hasLoadedVideos = false
    private var playerHeightBeforeFullscreen = ViewGroup.LayoutParams.WRAP_CONTENT

    // Receiver для кнопок уведомления
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                VideoPlayerService.ACTION_PLAY_PAUSE -> {
                    if (videoPlayerManager.isPlaying()) {
                        videoPlayerManager.pause()
                    } else {
                        videoPlayerManager.play()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        // Показываем загрузочный экран
        setContentView(R.layout.loading_screen)

        // Через 1 секунду переходим к основному интерфейсу
        Handler(Looper.getMainLooper()).postDelayed({
            setContentView(R.layout.activity_main)
            setupUI()
        }, 1000)
    }

    private fun setupUI() {
        Log.d(TAG, "setupUI called")

        // Инициализируем менеджер прогресса
        stateManager = VideoStateManager(this)

        // Инициализируем менеджер видеоплеера
        videoPlayerManager = VideoPlayerManager(this, stateManager)
        videoPlayerManager.initialize()

        // Инициализируем репозиторий
        repository = VideoRepository(this)

        // Находим View элементы
        playerView = findViewById(R.id.playerView)
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle)
        layoutQueueHeader = findViewById(R.id.layoutQueueHeader)
        btnShuffle = findViewById(R.id.btnShuffle)
        btnFullscreen = findViewById(R.id.btnFullscreen)
        loadingView = findViewById(R.id.loadingView)

        // Устанавливаем начальную видимость элементов
        playerView.visibility = View.VISIBLE // Плеер всегда виден
        tvPlayerTitle.text = "LocalTube"

        // Сохраняем начальную высоту плеера
        playerHeightBeforeFullscreen = playerView.layoutParams.height

        // Регистрируем receiver для кнопок уведомления
        val filter = IntentFilter().apply {
            addAction(VideoPlayerService.ACTION_PLAY_PAUSE)
        }
        registerReceiver(notificationReceiver, filter)

        // Настраиваем RecyclerView
        setupRecyclerView()

        // Настраиваем кнопку перемешивания
        setupShuffleButton()

        // Настраиваем кнопку полноэкранного режима
        setupFullscreenButton()

        // Проверяем и запрашиваем разрешения
        checkAndRequestPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val readPermissionGranted = grantResults.isNotEmpty() &&
                    grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

            if (readPermissionGranted) {
                // Проверяем, нужно ли запрашивать MANAGE_EXTERNAL_STORAGE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        // Все разрешения есть
                        loadVideos()
                    } else {
                        // Нужно запросить MANAGE_EXTERNAL_STORAGE
                        requestManageExternalStoragePermission()
                    }
                } else {
                    // Для старых версий Android
                    loadVideos()
                }
            } else {
                Toast.makeText(
                    this,
                    "Для работы приложения нужны разрешения на чтение видео",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE) {
            // Проверяем, предоставил ли пользователь разрешение
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadVideos()
                    Toast.makeText(this, "Разрешение на управление файлами предоставлено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "Без разрешения удаление видео будет недоступно",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById<RecyclerView>(R.id.rvVideos)

        adapter = VideoAdapter(emptyList()) { video ->
            playVideo(video)
        }

        adapter.setStateManager(stateManager)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Добавляем разделитель между элементами
        val divider = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider)!!)
        recyclerView.addItemDecoration(divider)

        // Настраиваем свайп для удаления видео
        setupSwipeToDelete()
    }

    private fun setupShuffleButton() {
        btnShuffle.setOnClickListener {
            shuffleVideos()
        }
    }

    private fun setupFullscreenButton() {
        btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }
    }

    private fun toggleFullscreen() {
        if (!isFullscreen) {
            enterFullscreen()
        } else {
            exitFullscreen()
        }
    }

    private fun setupSwipeToDelete() {
        swipeToDeleteCallback = SwipeToDeleteCallback(adapter, this) { position ->
            deleteVideo(position)
        }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun enterFullscreen() {
        Log.d(TAG, "enterFullscreen")
        isFullscreen = true

        // Сохраняем текущую высоту плеера
        playerHeightBeforeFullscreen = playerView.layoutParams.height

        // Меняем иконку кнопки
        btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)

        // Скрываем системный интерфейс
        hideSystemUI()

        // Скрываем другие элементы интерфейса
        tvPlayerTitle.visibility = View.GONE
        layoutQueueHeader.visibility = View.GONE
        recyclerView.visibility = View.GONE

        // Меняем высоту плеера на match_parent
        val params = playerView.layoutParams
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        playerView.layoutParams = params

        // Пересчитываем layout
        playerView.requestLayout()

        // Меняем ориентацию на горизонтальную
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun exitFullscreen() {
        Log.d(TAG, "exitFullscreen")
        isFullscreen = false

        // Меняем иконку кнопки обратно
        btnFullscreen.setImageResource(R.drawable.ic_fullscreen)

        // Показываем системный интерфейс
        showSystemUI()

        // Показываем элементы интерфейса
        tvPlayerTitle.visibility = View.VISIBLE
        layoutQueueHeader.visibility = View.VISIBLE
        recyclerView.visibility = View.VISIBLE

        // Восстанавливаем высоту плеера
        val params = playerView.layoutParams
        params.height = playerHeightBeforeFullscreen
        playerView.layoutParams = params

        // Пересчитываем layout
        playerView.requestLayout()

        // Возвращаем портретную ориентацию
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }

    private fun shuffleVideos() {
        if (allVideos.isEmpty()) return

        val shuffledList = allVideos.shuffled()
        adapter.updateVideos(shuffledList)
        Toast.makeText(this, "Очередь перемешана", Toast.LENGTH_SHORT).show()
    }

    private fun checkAndRequestPermissions() {
        if (hasAllPermissions()) {
            loadVideos()
        } else {
            requestPermissions()
        }
    }

    private fun hasAllPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                    (!needsManageExternalStorage() || Environment.isExternalStorageManager())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    (!needsManageExternalStorage() || Environment.isExternalStorageManager())
        } else {
            // Android 10 и ниже
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun needsManageExternalStorage(): Boolean {
        // На Android 11+ для удаления файлов нужно MANAGE_EXTERNAL_STORAGE
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                PERMISSION_REQUEST_CODE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Android 10 и ниже
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AlertDialog.Builder(this)
                .setTitle("Разрешение на управление файлами")
                .setMessage("Для удаления видеофайлов необходимо предоставить разрешение на управление всеми файлами. Вы будете перенаправлены в настройки.")
                .setPositiveButton("Перейти в настройки") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${packageName}")
                        startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE)
                    } catch (e: Exception) {
                        // Альтернативный способ для некоторых устройств
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE)
                    }
                }
                .setNegativeButton("Позже") { _, _ ->
                    Toast.makeText(
                        this,
                        "Вы можете предоставить разрешение позже в настройках приложения",
                        Toast.LENGTH_LONG
                    ).show()
                    loadVideos() // Загружаем видео без возможности удаления
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun deleteVideo(position: Int) {
        val video = adapter.getVideoAtPosition(position)

        // Проверяем, является ли видео текущим
        if (currentPlayingVideo?.id == video.id) {
            currentPlayingVideo = null
            tvPlayerTitle.text = "LocalTube"
        }

        // Проверяем разрешения
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()) {
            Toast.makeText(this, "Для удаления видео нужно разрешение на управление файлами", Toast.LENGTH_LONG).show()
            requestManageExternalStoragePermission()
            adapter.notifyItemChanged(position)
            return
        }

        Thread {
            try {
                val deleted = repository.deleteVideo(video)

                runOnUiThread {
                    if (deleted) {
                        // ЛОКАЛЬНО удаляем видео из всех списков
                        allVideos = allVideos.filter { it.id != video.id }
                        adapter.removeVideoById(video.id) // Новый метод в адаптере

                        // Проверяем, не пустой ли список
                        if (adapter.itemCount == 0) {
                            recyclerView.visibility = View.GONE
                            layoutQueueHeader.visibility = View.GONE
                        }

                        Toast.makeText(this, "Видео удалено", Toast.LENGTH_SHORT).show()
                    } else {
                        adapter.notifyItemChanged(position)
                        Toast.makeText(this, "Не удалось удалить видео", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    adapter.notifyItemChanged(position)
                    Toast.makeText(this, "Ошибка при удалении: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun loadVideos() {
        Log.d(TAG, "loadVideos called, hasLoadedVideos: $hasLoadedVideos")

        if (hasLoadedVideos) {
            Log.d(TAG, "Videos already loaded, skipping")
            return
        }

        if (!this::loadingView.isInitialized) return
        loadingView.isVisible = true

        Thread {
            try {
                val videos = repository.getAllVideos()
                allVideos = videos

                runOnUiThread {
                    loadingView.isVisible = false
                    hasLoadedVideos = true

                    if (videos.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        layoutQueueHeader.visibility = View.GONE

                        Toast.makeText(
                            this,
                            "Видео не найдены\n\nПереместите видеофайлы в папку Movies/LocalTube на вашем устройстве",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        layoutQueueHeader.visibility = View.VISIBLE

                        adapter.updateVideos(videos)

                        // Воспроизводим последнее видео только если приложение активно
                        if (isActivityVisible) {
                            autoPlayVideo(videos)
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loadingView.isVisible = false
                    Toast.makeText(
                        this,
                        "Ошибка загрузки видео: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun autoPlayVideo(videos: List<ru.f1cha.localtube.data.Video>) {
        if (videos.isEmpty()) return

        // Проверяем, не воспроизводится ли уже видео
        if (videoPlayerManager.getCurrentVideo() != null) {
            Log.d(TAG, "Video already playing, skipping autoPlay")
            return
        }

        val lastVideoId = videoPlayerManager.getLastVideoId()
        if (lastVideoId > 0) {
            val lastVideo = videos.find { it.id == lastVideoId }
            if (lastVideo != null) {
                Log.d(TAG, "Resuming last video: ${lastVideo.title}")
                prepareVideo(lastVideo, false) // Подготавливаем, но не воспроизводим
                return
            }
        }

        Log.d(TAG, "No auto-play, waiting for user interaction")
    }

    private fun prepareVideo(video: ru.f1cha.localtube.data.Video, autoPlay: Boolean = true) {
        Log.d(TAG, "prepareVideo: ${video.title}, autoPlay: $autoPlay")

        currentPlayingVideo = video
        adapter.setCurrentPlayingVideoId(video.id)

        tvPlayerTitle.text = video.title

        // Всегда показываем плеер, но не меняем его размер
        playerView.visibility = View.VISIBLE

        // Подготавливаем видео в плеере
        if (autoPlay) {
            videoPlayerManager.preparePlayer(playerView, video)
        } else {
            // Только подготавливаем, но не воспроизводим
            videoPlayerManager.preparePlayerWithoutAutoPlay(playerView, video)
        }
    }

    private fun playVideo(video: ru.f1cha.localtube.data.Video) {
        Log.d(TAG, "playVideo: ${video.title}")

        // Если это то же самое видео, которое уже загружено
        if (currentPlayingVideo?.id == video.id) {
            // Если видео уже играет, ничего не делаем, если на паузе - возобновляем
            if (!videoPlayerManager.isPlaying()) {
                videoPlayerManager.play()
            }
            return
        }

        // Иначе подготавливаем и воспроизводим новое видео
        prepareVideo(video, true)
    }

    override fun onStart() {
        super.onStart()
        isActivityVisible = true
        Log.d(TAG, "onStart, isActivityVisible: $isActivityVisible")
    }

    override fun onStop() {
        super.onStop()
        isActivityVisible = false
        Log.d(TAG, "onStop, isActivityVisible: $isActivityVisible")

        currentPlayingVideo?.let { video ->
            videoPlayerManager.saveCurrentPosition(video.id)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")

        currentPlayingVideo?.let { video ->
            videoPlayerManager.saveCurrentPosition(video.id)
        }
        videoPlayerManager.release()

        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver не был зарегистрирован, игнорируем
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        Log.d(TAG, "onResume, isActivityVisible: $isActivityVisible")

        // Загружаем видео только если они еще не загружены
        if (hasAllPermissions() && !hasLoadedVideos) {
            loadVideos()
        }
    }

    override fun onBackPressed() {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            super.onBackPressed()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged, orientation: ${newConfig.orientation}")

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (isFullscreen) {
                exitFullscreen()
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Если не в полноэкранном режиме, не меняем размер плеера
            if (!isFullscreen) {
                val params = playerView.layoutParams
                params.height = playerHeightBeforeFullscreen
                playerView.layoutParams = params
            }
        }
    }
}