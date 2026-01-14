package ru.f1cha.localtube

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ru.f1cha.localtube.data.VideoRepository

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DELAY = 2000L  // 2 секунды минимум
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Скрываем статус-бар на время сплэш-скрина
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_splash)

        // Запускаем анимации
        startAnimations()

        // Загружаем видео в фоне
        loadVideosInBackground()

        // Переходим к MainActivity через 2 секунды (минимум)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()  // Закрываем сплэш-активность
        }, SPLASH_DELAY)
    }

    private fun startAnimations() {
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val appNameTextView = findViewById<TextView>(R.id.appNameTextView)
        val taglineTextView = findViewById<TextView>(R.id.taglineTextView)

        // Анимация появления логотипа
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000
        logoImageView.startAnimation(fadeIn)

        // Анимация появления названия
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        slideUp.duration = 800
        slideUp.startOffset = 300
        appNameTextView.startAnimation(slideUp)

        // Анимация появления подзаголовка
        val slideUpDelayed = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        slideUpDelayed.duration = 800
        slideUpDelayed.startOffset = 600
        taglineTextView.startAnimation(slideUpDelayed)
    }

    private fun loadVideosInBackground() {
        // Запускаем в отдельном потоке предварительную загрузку видео
        Thread {
            try {
                val repository = VideoRepository(this)
                repository.getAllVideos()  // Просто вызываем, чтобы кэшировать
            } catch (e: Exception) {
                // Игнорируем ошибки на сплэш-скрине
                e.printStackTrace()
            }
        }.start()
    }

    override fun onBackPressed() {
        // Отключаем кнопку "Назад" на сплэш-скрине
        // super.onBackPressed()  // Закомментировано
    }
}