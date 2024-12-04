package com.example.guessgame

class CompletionRateCalculator {
    companion object {
        init {
            System.loadLibrary("completion_rate")  // Имя вашей библиотеки
        }


        @JvmStatic external fun calculateCompletionRate(completedTasks: Int, totalTasks: Int): Int
    }
}
