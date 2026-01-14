package ru.f1cha.localtube.data

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity) {

    companion object {
        // Код для запроса разрешений (можем менять)
        const val REQUEST_PERMISSIONS_CODE = 100

        // Разрешения, которые нужны для чтения видео
        private fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+
                arrayOf(
                    Manifest.permission.READ_MEDIA_VIDEO
                    // Удаление через MediaStore не требует отдельного разрешения
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11-12
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                    // Удаление через MediaStore не требует отдельного разрешения
                )
            } else {
                // Android 5-10 - нужны оба разрешения
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    // Проверяем, есть ли все необходимые разрешения
    fun hasAllPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Запрашиваем разрешения
    fun requestPermissions(requestCode: Int = REQUEST_PERMISSIONS_CODE) {
        ActivityCompat.requestPermissions(
            activity,
            getRequiredPermissions(),
            requestCode
        )
    }

    // Обрабатываем результат запроса разрешений
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            return grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        }
        return false
    }

    // Показываем объяснение, зачем нужно разрешение
    fun shouldShowRationale(): Boolean {
        return getRequiredPermissions().any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    // Получаем список необходимых разрешений (для отладки)
    fun getRequiredPermissionsList(): List<String> {
        return getRequiredPermissions().toList()
    }
}