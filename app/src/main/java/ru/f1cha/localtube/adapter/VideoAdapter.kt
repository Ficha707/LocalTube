package ru.f1cha.localtube.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.f1cha.localtube.R
import ru.f1cha.localtube.data.Video
import ru.f1cha.localtube.data.VideoStateManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity

class VideoAdapter(
    private var videos: List<Video>,
    private val onItemClick: (Video) -> Unit,
    private val onAddToPlaylist: (Video) -> Unit,
    private val onDeleteVideo: (Video, callback: (Boolean) -> Unit) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private lateinit var stateManager: VideoStateManager
    private var currentPlayingVideoId: Long = -1

    fun setStateManager(manager: VideoStateManager) {
        stateManager = manager
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

            // Загружаем превью через кэш
            ivThumbnail.loadVideoThumbnail(itemView.context, video.id)

            // Получаем прогресс просмотра
            val savedPosition = stateManager.getProgress(video.id)
            val progressPercentage = if (savedPosition > 0 && video.duration > 0) {
                ((savedPosition * 100) / video.duration).toInt()
            } else {
                0
            }

            // Настраиваем прогресс-бар
            if (progressPercentage > 0) {
                progressWatched.visibility = View.VISIBLE
                progressWatched.progress = progressPercentage
            } else {
                progressWatched.visibility = View.GONE
            }

            // Подсветка текущего воспроизводимого видео
            if (video.id == currentPlayingVideoId) {
                itemView.setBackgroundColor(Color.parseColor("#2A2A2A"))
            } else {
                itemView.setBackgroundColor(Color.parseColor("#000000"))
            }

            // Обработка клика - открытие плеера
            itemView.setOnClickListener {
                onItemClick(video)
            }

            // Обработка долгого нажатия - контекстное меню
            itemView.setOnLongClickListener {
                showContextMenu(video, it)
                true
            }
        }

        /**
         * Показывает контекстное меню при долгом нажатии
         */
        private fun showContextMenu(video: Video, anchor: View) {
            val popup = PopupMenu(itemView.context, anchor)
            popup.menuInflater.inflate(R.menu.video_context_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_add_to_playlist -> {
                        onAddToPlaylist(video)
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog(video)
                        true
                    }
                    R.id.action_info -> {
                        showVideoInfoDialog(video)
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }

        /**
         * Диалог подтверждения удаления
         */
        private fun showDeleteConfirmationDialog(video: Video) {
            androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                .setTitle("Удаление видео")
                .setMessage("Удалить видео \"${video.title}\"?")
                .setPositiveButton("Удалить") { dialog, _ ->
                    dialog.dismiss()
                    onDeleteVideo(video) { success ->
                        if (success) {
                            // Удаляем локально из списка
                            val position = adapterPosition
                            if (position != RecyclerView.NO_POSITION) {
                                // Уведомляем адаптер об удалении
                                // (будет обработано в MainActivity)
                            }
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        /**
         * Диалог с информацией о видео
         */
        private fun showVideoInfoDialog(video: Video) {
            val info = """
                Название: ${video.title}
                Длительность: ${video.getFormattedDuration()}
                Размер: ${video.getFormattedSize()}
                Путь: ${video.path}
                Дата добавления: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(java.util.Date(video.dateAdded))}
            """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                .setTitle("Информация о видео")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show()
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