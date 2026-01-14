package ru.f1cha.localtube.adapter

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ru.f1cha.localtube.R
import android.view.View

class SwipeToDeleteCallback(
    private val adapter: VideoAdapter,
    private val context: Context,
    private val onDelete: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val background = ColorDrawable(Color.parseColor("#FF3B30"))
    private var swipedPosition = -1
    private var isSwipedEnough = false
    private var swipeThreshold = 0.5f // Порог свайпа для автоматического возврата

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false // Не поддерживаем перетаскивание
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        swipedPosition = viewHolder.adapterPosition

        // Показываем диалог подтверждения удаления
        val video = adapter.getVideoAtPosition(swipedPosition)
        showDeleteConfirmationDialog(video.title) { confirmed ->
            if (confirmed) {
                onDelete(swipedPosition)
            } else {
                // Если отмена, возвращаем элемент на место
                adapter.notifyItemChanged(swipedPosition)
                swipedPosition = -1
            }
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return swipeThreshold
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        // Делаем возврат менее чувствительным
        return defaultValue * 0.5f
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Рисуем красный фон при свайпе
            if (dX < 0) { // Свайп влево
                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(canvas)
            }
        }

        // Продолжаем стандартную отрисовку
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        swipedPosition = -1
    }

    private fun showDeleteConfirmationDialog(videoTitle: String, callback: (Boolean) -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Удаление видео")
            .setMessage("Удалить видео \"$videoTitle\"?")
            .setPositiveButton("Удалить") { dialog, _ ->
                dialog.dismiss()
                callback(true)
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
                callback(false)
            }
            .setCancelable(false)
            .show()
    }

    // Метод для сброса свайпа
    fun resetSwipe() {
        swipedPosition = -1
    }
}