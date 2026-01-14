package ru.f1cha.localtube.data

import android.content.Context
import android.provider.MediaStore
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

object ThumbnailLoader {

    fun loadVideoThumbnail(context: Context, videoId: Long, imageView: android.widget.ImageView) {
        try {
            // Создаем URI для видео через MediaStore
            val uri = android.content.ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoId
            )

            // Настройки для загрузки - УВЕЛИЧИВАЕМ размер превью
            val requestOptions = RequestOptions()
                .placeholder(android.R.drawable.ic_media_play)
                .error(android.R.drawable.ic_media_play)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(240, 160) // Увеличиваем в 2 раза (было 120x80)
                .centerCrop()

            // Загружаем через Glide
            Glide.with(context)
                .asBitmap()
                .load(uri)
                .apply(requestOptions)
                .into(imageView)

        } catch (e: Exception) {
            imageView.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    fun loadVideoThumbnail(context: Context, videoPath: String, imageView: android.widget.ImageView) {
        try {
            val requestOptions = RequestOptions()
                .placeholder(android.R.drawable.ic_media_play)
                .error(android.R.drawable.ic_media_play)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(240, 160) // Увеличиваем в 2 раза
                .centerCrop()

            Glide.with(context)
                .asBitmap()
                .load(videoPath)
                .apply(requestOptions)
                .into(imageView)

        } catch (e: Exception) {
            imageView.setImageResource(android.R.drawable.ic_media_play)
        }
    }
}