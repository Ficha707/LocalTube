package ru.f1cha.localtube.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    companion object {
        const val REQUEST_CODE_READ_MEDIA = 100
        const val REQUEST_CODE_MANAGE_STORAGE = 101
        const val REQUEST_CODE_POST_NOTIFICATIONS = 102
        const val REQUEST_CODE_PICTURE_IN_PICTURE = 103
    }

    // Проверяем все необходимые разрешения для приложения
    fun checkAllPermissions(): Boolean {
        return hasReadMediaPermission() &&
                hasNotificationPermission() &&
                (!needsManageStorage() || hasManageStoragePermission())
    }

    // Разрешение на чтение медиа (разное для разных версий Android)
    fun hasReadMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 10 и ниже
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Разрешение на уведомления (Android 13+)
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Для версий ниже 13 не требуется
        }
    }

    // Управление всеми файлами (Android 11+ для удаления)
    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun needsManageStorage(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    // Запрашиваем все необходимые разрешения
    fun requestAllPermissions(activity: Activity) {
        val permissionsToRequest = mutableListOf<String>()

        // Разрешение на чтение медиа
        if (!hasReadMediaPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // Разрешение на уведомления (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission()) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                REQUEST_CODE_READ_MEDIA
            )
        }

        // Запрос на управление файлами (отдельно, так как требует настройки)
        if (needsManageStorage() && !hasManageStoragePermission()) {
            showManageStorageDialog(activity)
        }
    }

    private fun showManageStorageDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Управление файлами")
            .setMessage("Для удаления видео необходимо предоставить разрешение на управление всеми файлами. Это можно сделать в настройках.")
            .setPositiveButton("Настройки") { _, _ ->
                openManageStorageSettings(activity)
            }
            .setNegativeButton("Позже", null)
            .show()
    }

    private fun openManageStorageSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
        }
    }

    // Обработка результата запроса разрешений
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        return when (requestCode) {
            REQUEST_CODE_READ_MEDIA -> {
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            }
            REQUEST_CODE_MANAGE_STORAGE -> {
                hasManageStoragePermission()
            }
            else -> false
        }
    }
}