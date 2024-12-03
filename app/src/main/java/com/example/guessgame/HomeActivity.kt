package com.example.guessgame

import android.app.TimePickerDialog
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
import android.content.Intent
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

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


data class TaskToUpdate(
    val id: Int,
    val title: String,
    val description: String,
    val priority: String,
    val deadline: String
)
data class TaskToCreate(
    val title: String,
    val description: String,
    val priority: String,
    val deadline: String
)

class TaskAdapter(val tasks: MutableList<Task>,  val onTaskClick: (Task) -> Unit,
                  val onTaskCompleted: (Int) -> Unit) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView: TextView = itemView.findViewById(R.id.taskTitle)
        val descriptionView: TextView = itemView.findViewById(R.id.taskDescription)
        val priorityView: TextView = itemView.findViewById(R.id.taskPriority)
        val deadlineView: TextView = itemView.findViewById(R.id.taskDeadline)
        val taskItem: View = itemView.findViewById(R.id.taskItem)

        init {
            taskItem.setOnClickListener {
                val task = tasks[adapterPosition]
                onTaskClick(task) // вызываем onTaskClick, когда задача нажата
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.titleView.text = task.title
        holder.descriptionView.text = "Description: ${task.description}"
        holder.priorityView.text = "Priority: ${task.priority}"
        holder.deadlineView.text = "Deadline: ${formatDate(task.deadline)}"

        if (task.completed) {
            holder.taskItem.setBackgroundColor(0xFFE0F7FA.toInt())
        } else {
            holder.taskItem.setBackgroundColor(0xFFFFEBEE.toInt())
        }
    }

    override fun getItemCount() = tasks.size

    private fun formatDate(dateString: String): String {
        // Используем SimpleDateFormat для преобразования формата
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        return try {
            val date = inputFormat.parse(dateString)
            if (date != null) {
                outputFormat.format(date)
            } else {
                dateString // Возвращаем оригинальную строку, если парсинг не удался
            }
        } catch (e: Exception) {
            e.printStackTrace()
            dateString // Возвращаем оригинальную строку в случае ошибки
        }
    }

    fun markTaskAsCompleted(position: Int, context: Context) {
        val task = tasks[position]
        if (task.completed) {
            // Если задача уже выполнена, сбрасываем состояние свайпа
            notifyItemChanged(position)
            Toast.makeText(context, "Эта задача уже помечена как выполненная", Toast.LENGTH_SHORT).show()
        } else {
            // Помечаем задачу как выполненную, если она не была завершена
            task.completed = true
            onTaskCompleted(task.id)
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
        val fabAddTask: FloatingActionButton = findViewById(R.id.fabAddTask)
        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.menu_tasks
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_tasks -> {
                    // Уже текущий экран, ничего не делаем
                    true
                }

                R.id.menu_analytics -> {
                    // Переход на экран Analytics
                    val intent = Intent(this, AnalyticsActivity::class.java)
                    val totalTasks = taskAdapter.itemCount
                    val completedTasks = taskAdapter.tasks.count { it.completed }

                    intent.putExtra("totalTasks", totalTasks)
                    intent.putExtra("completedTasks", completedTasks)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
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
        taskAdapter = TaskAdapter(tasks.toMutableList(), { task ->
            showEditTaskDialog(task)
        }, {taskId -> markTaskAsCompleted(taskId)})
        recyclerView.adapter = taskAdapter
    }
    private fun showEditTaskDialog(task: Task) {
        // Inflating the layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null)

        // Pre-filling the EditText fields with current task data
        dialogView.findViewById<EditText>(R.id.editTextTaskTitle).setText(task.title)
        dialogView.findViewById<EditText>(R.id.editTextTaskDescription).setText(task.description)
        dialogView.findViewById<EditText>(R.id.editTextTaskPriority).setText(task.priority)
        dialogView.findViewById<EditText>(R.id.editTextTaskDeadline).setText(formatDate(task.deadline).split(" ")[0]) // Assuming the date is at index 0
        dialogView.findViewById<EditText>(R.id.editTextTaskTime).setText(formatDate(task.deadline).split(" ")[1]) // Assuming the time is at index 1

        // Building the dialog
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit Task")
            .setPositiveButton("Save") { dialog, _ ->
                // Getting updated task data from the dialog's EditText fields
                val updatedTitle = dialogView.findViewById<EditText>(R.id.editTextTaskTitle).text.toString()
                val updatedDescription = dialogView.findViewById<EditText>(R.id.editTextTaskDescription).text.toString()
                val updatedPriority = dialogView.findViewById<EditText>(R.id.editTextTaskPriority).text.toString()
                val updatedDeadline = dialogView.findViewById<EditText>(R.id.editTextTaskDeadline).text.toString()
                val updatedTime = dialogView.findViewById<EditText>(R.id.editTextTaskTime).text.toString()

                // Validating input fields before sending data to the server
                if (updatedTitle.isNotBlank() && updatedDescription.isNotBlank() && updatedDeadline.isNotBlank() && updatedTime.isNotBlank()) {
                    val fullDeadline = formatDateTime(updatedDeadline, updatedTime)
                    updateTask(task.id, updatedTitle, updatedDescription, updatedPriority, fullDeadline)
                } else {
                    Toast.makeText(this, "Title, Description, Deadline, and Time are required", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        dialogBuilder.create().show()
    }
    private fun markTaskAsCompleted(taskId: Int) {
        val url = "${Constants.BASE_URL}${Constants.TASKS_URL}/$taskId"
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token == null) {
            Toast.makeText(this, "Необходимо войти в систему", Toast.LENGTH_SHORT).show()
            return
        }

        // Создаем объект с только одним полем "completed"
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            """{"completed": true}"""
        )

        // Создаем запрос PUT
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .put(requestBody)
            .build()

        // Отправляем запрос
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Ошибка при обновлении задачи", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Задача обновлена", Toast.LENGTH_SHORT).show()
                        fetchTasks() // Обновляем список задач после редактирования
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Ошибка при обновлении задачи", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun updateTask(taskId: Int, title: String, description: String, priority: String, deadline: String) {
        val url = "${Constants.BASE_URL}${Constants.TASKS_URL}/$taskId"
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token == null) {
            Toast.makeText(this, "Необходимо войти в систему", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedTask = TaskToUpdate(taskId, title, description, priority, deadline)
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            gson.toJson(updatedTask)
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .put(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Ошибка при обновлении задачи", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Задача обновлена", Toast.LENGTH_SHORT).show()
                        fetchTasks() // Обновляем список задач после редактирования
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Ошибка при обновлении задачи", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add New Task")
            .setPositiveButton("Create") { dialog, _ ->

                val title = dialogView.findViewById<EditText>(R.id.editTextTaskTitle).text.toString()
                val description = dialogView.findViewById<EditText>(R.id.editTextTaskDescription).text.toString()
                val priority = dialogView.findViewById<EditText>(R.id.editTextTaskPriority).text.toString()
                val deadline = dialogView.findViewById<EditText>(R.id.editTextTaskDeadline).text.toString()
                val time = dialogView.findViewById<EditText>(R.id.editTextTaskTime).text.toString()

                if (title.isNotBlank() && description.isNotBlank() && deadline.isNotBlank() && time.isNotBlank()) {
                    val fullDeadline = formatDateTime(deadline, time) // Форматируем дату и время в нужный формат
                    createTask(title, description, priority, fullDeadline)
                } else {
                    Toast.makeText(this, "Title, Description, Deadline, and Time are required", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val timeEditText: EditText = dialogView.findViewById(R.id.editTextTaskTime)
        timeEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                    // Формируем строку времени
                    val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                    timeEditText.setText(timeString) // Устанавливаем выбранное время
                },
                hour,
                minute,
                true
            )
            timePickerDialog.show() // Показываем диалог для выбора времени
        }

        dialogBuilder.create().show()
    }

    // Функция для форматирования даты и времени
    private fun formatDateTime(date: String, time: String): String {
        // Формируем строку из даты и времени
        val dateTimeString = "$date $time"

        // Используем SimpleDateFormat для форматирования в нужный формат
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

        // Парсим строку с датой и временем в объект Date
        val dateObject = inputFormat.parse(dateTimeString)

        // Преобразуем в строку в нужном формате
        return outputFormat.format(dateObject)
    }
    private fun formatDate(dateString: String): String {
        // Используем SimpleDateFormat для преобразования формата
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        return try {
            val date = inputFormat.parse(dateString)
            if (date != null) {
                outputFormat.format(date)
            } else {
                dateString // Возвращаем оригинальную строку, если парсинг не удался
            }
        } catch (e: Exception) {
            e.printStackTrace()
            dateString // Возвращаем оригинальную строку в случае ошибки
        }
    }

    private fun createTask(title: String, description: String, priority: String, deadline: String) {
        val url = Constants.BASE_URL + Constants.TASKS_URL
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token == null) {
            Toast.makeText(this, "Необходимо войти в систему", Toast.LENGTH_SHORT).show()
            return
        }

        val task = TaskToCreate(
            title = title,
            description = description,
            priority = priority,
            deadline = deadline,
        )
        Log.d("HomeActivity", "Task added: ${task.title}, Deadline: ${task.deadline}")

        val workManager = WorkManager.getInstance(this)

        // Создаем InputData с данными о задаче
        val inputData = workDataOf(
            NotificationWorker.TASK_TITLE_KEY to task.title,
            NotificationWorker.TASK_DEADLINE_KEY to formatDate(task.deadline)
        )

        Log.d("HomeActivity", "Scheduling notification for task: ${task.title} in 1 seconds")
        val notificationWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(1, TimeUnit.SECONDS)
            .setInputData(inputData)
            .build()

        // Отправляем задачу в WorkManager
        workManager.enqueue(notificationWorkRequest)
        Log.d("HomeActivity", "Work scheduled for task: ${task.title}")
        val jsonBody = gson.toJson(task)
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), jsonBody)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@HomeActivity, "Ошибка при создании задачи", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Задача успешно создана", Toast.LENGTH_SHORT).show()
                        fetchTasks() // обновляем список задач
                    }
                } else {
                    runOnUiThread {
                        Log.d("response", response.message)
                        Toast.makeText(this@HomeActivity, "Ошибка на сервере", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}