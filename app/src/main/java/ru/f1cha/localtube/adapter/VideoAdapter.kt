package ru.f1cha.localtube.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.f1cha.localtube.R
import ru.f1cha.localtube.data.ThumbnailLoader
import ru.f1cha.localtube.data.Video
import ru.f1cha.localtube.data.VideoStateManager
import android.graphics.Color

class VideoAdapter(

    private var videos: List<Video>,
    private val onItemClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private lateinit var stateManager: VideoStateManager
    private var currentPlayingVideoId: Long = -1

    fun setStateManager(manager: VideoStateManager) {
        stateManager = manager
    }

    fun setCurrentPlayingVideoId(videoId: Long) {
        this.currentPlayingVideoId = videoId
        notifyDataSetChanged()
    }

    fun updateVideos(newVideos: List<Video>) {
        this.videos = newVideos
        notifyDataSetChanged()
    }

    fun getVideoAtPosition(position: Int): Video {
        return videos[position]
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvSize: TextView = itemView.findViewById(R.id.tvSize)
        private val progressWatched: ProgressBar = itemView.findViewById(R.id.progressWatched)

        fun bind(video: Video) {
            tvTitle.text = video.title
            tvDuration.text = video.getFormattedDuration()
            tvSize.text = video.getFormattedSize()

            // Папку убрали из отображения
            // tvFolder.text = video.folderName ?: "Без папки"

            // Загружаем превью видео
            ThumbnailLoader.loadVideoThumbnail(itemView.context, video.id, ivThumbnail)

            // Получаем прогресс просмотра через VideoStateManager
            val savedPosition = stateManager.getProgress(video.id)
            val progressPercentage = if (savedPosition > 0 && video.duration > 0) {
                ((savedPosition * 100) / video.duration).toInt()
            } else {
                0
            }

            // Настраиваем красный прогресс-бар
            if (progressPercentage > 0) {
                progressWatched.visibility = View.VISIBLE
                progressWatched.progress = progressPercentage
            } else {
                progressWatched.visibility = View.GONE
            }

            // Подсветка текущего воспроизводимого видео (меняем фон)
            if (video.id == currentPlayingVideoId) {
                itemView.setBackgroundColor(Color.parseColor("#2A2A2A")) // Светлее чёрного
            } else {
                itemView.setBackgroundColor(Color.parseColor("#000000")) // Чёрный
            }

            itemView.setOnClickListener {
                onItemClick(video)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
    }

    override fun getItemCount(): Int = videos.size
}