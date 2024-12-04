package com.example.guessgame

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class AnalyticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        val totalTasks = intent.getIntExtra("totalTasks", 0)
        val completedTasks = intent.getIntExtra("completedTasks", 0)
        val remainingTasks = totalTasks - completedTasks
        val completionRate = CompletionRateCalculator.calculateCompletionRate(completedTasks, totalTasks);

        findViewById<TextView>(R.id.totalTasksView).text = "Total Tasks: $totalTasks"
        findViewById<TextView>(R.id.completedTasksView).text = "Completed Tasks: $completedTasks"
        findViewById<TextView>(R.id.remainingTasksView).text = "Remaining Tasks: $remainingTasks"
        findViewById<TextView>(R.id.completionRateView).text = "Completion Rate: $completionRate%"

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.menu_analytics // Устанавливаем активный элемент

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_tasks -> {
                    // Переход в HomeActivity
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_analytics -> true // Уже на этом экране
                else -> false
            }
        }
    }
}
