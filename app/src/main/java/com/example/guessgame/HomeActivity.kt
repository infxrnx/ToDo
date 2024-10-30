package com.example.guessgame

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

open class OnSwipeTouchListener(ctx: Context) : View.OnTouchListener {
    private val gestureDetector = GestureDetector(ctx, GestureListener())

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffY = e2.y - (e1?.y ?: 0f)
            val diffX = e2.x - (e1?.x ?: 0f)

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) onSwipeRight() else onSwipeLeft()
                }
            } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) onSwipeDown() else onSwipeUp()
            }
            return true
        }
    }

    open fun onSwipeRight() {
        Log.d("Swipe", "Right")
    }
    open fun onSwipeLeft() {
        Log.d("Swipe", "Left")
    }
    open fun onSwipeUp() {
        Log.d("Swipe", "Up")
    }
    open fun onSwipeDown() {
        Log.d("Swipe", "Down")
    }

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}

data class Task(
    val id: Int,
    val userId: Int,
    val title: String,
    val description: String,
    var completed: Boolean,
    val priority: String,
    val deadline: String, // Строка для удобного отображения даты
    val createdAt: String
)

class TaskAdapter(private val tasks: MutableList<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView: TextView = itemView.findViewById(R.id.taskTitle)
        val descriptionView: TextView = itemView.findViewById(R.id.taskDescription)
        val priorityView: TextView = itemView.findViewById(R.id.taskPriority)
        val deadlineView: TextView = itemView.findViewById(R.id.taskDeadline)
        val taskItem: View = itemView.findViewById(R.id.taskItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.titleView.text = task.title
        holder.descriptionView.text = task.description
        holder.priorityView.text = "Priority: ${task.priority}"
        holder.deadlineView.text = "Deadline: ${task.deadline}"

        if (task.completed) {
            holder.taskItem.setBackgroundColor(0xFFE0F7FA.toInt())
        } else {
            holder.taskItem.setBackgroundColor(0xFFFFEBEE.toInt())
        }
    }

    override fun getItemCount() = tasks.size

    fun markTaskAsCompleted(position: Int, context: Context) {
        val task = tasks[position]
        if (task.completed) {
            // Если задача уже выполнена, сбрасываем состояние свайпа
            notifyItemChanged(position)
            Toast.makeText(context, "Эта задача уже помечена как выполненная", Toast.LENGTH_SHORT).show()
        } else {
            // Помечаем задачу как выполненную, если она не была завершена
            task.completed = true
            notifyItemChanged(position)
            Toast.makeText(context, "Задача помечена как выполненная", Toast.LENGTH_SHORT).show()
        }
    }
}


class HomeActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val gson = Gson()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout // Объявляем SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Инициализация SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchTasks() // Загружаем данные при свайпе вниз
        }

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Загружаем задачи
        fetchTasks()

        // Настраиваем свайп вправо для выполнения задачи
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // перетаскивание не используется
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                taskAdapter.markTaskAsCompleted(position, this@HomeActivity)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun fetchTasks() {
        val url = Constants.BASE_URL + Constants.TASKS_URL
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token == null) {
            Toast.makeText(this, "Необходимо войти в систему", Toast.LENGTH_SHORT).show()
            swipeRefreshLayout.isRefreshing = false // Останавливаем анимацию при ошибке
            return
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Ошибка при получении данных", Toast.LENGTH_LONG).show()
                    swipeRefreshLayout.isRefreshing = false // Останавливаем анимацию при ошибке
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        try {
                            val taskType = object : TypeToken<List<Task>>() {}.type
                            val tasks: List<Task> = gson.fromJson(responseBody, taskType)
                            runOnUiThread {
                                displayTasks(tasks)
                                swipeRefreshLayout.isRefreshing = false // Останавливаем анимацию после загрузки данных
                            }
                        } catch (e: Exception) {
                            Log.e("HomeActivity", "Ошибка при парсинге JSON: ${e.message}")
                            runOnUiThread {
                                Toast.makeText(this@HomeActivity, "Ошибка при обработке данных", Toast.LENGTH_LONG).show()
                                swipeRefreshLayout.isRefreshing = false // Останавливаем анимацию при ошибке
                            }
                        }
                    }
                } else {
                    Log.e("HomeActivity", "Ошибка на сервере: ${response.code}, сообщение: ${response.message}")
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Ошибка на сервере", Toast.LENGTH_LONG).show()
                        swipeRefreshLayout.isRefreshing = false // Останавливаем анимацию при ошибке
                    }
                }
            }
        })
    }

    private fun displayTasks(tasks: List<Task>) {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        taskAdapter = TaskAdapter(tasks.toMutableList()) // изменяем на MutableList
        recyclerView.adapter = taskAdapter
    }
}
