package com.example.guessgame

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException
import android.util.Log
import android.widget.TextView
import okhttp3.MediaType.Companion.toMediaType
import android.widget.Toast
import org.json.JSONObject // Импортируем JSONObject для работы с JSON

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val signinButton: Button = findViewById(R.id.signinButton)
        val signupButton: Button = findViewById(R.id.signupButton)
        val usernameEditText: EditText = findViewById(R.id.usernameText)
        val passwordEditText: EditText = findViewById(R.id.passwordText)

        signinButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            Log.d("MainActivity", "Логин: $username, Пароль: $password")
            sendLoginRequest(username, password)
        }

        signupButton.setOnClickListener {
            // Переход на новую активность SignupActivity
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

    }

    private fun sendLoginRequest(username: String, password: String) {
        val url = Constants.BASE_URL + Constants.LOGIN_URL
        val json = """
            {
                "username": "$username",
                "password": "$password"
            }
        """.trimIndent()

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        Log.d("MainActivity", "Запрос отправляется на сервер: $url")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Логирование ошибки
                Log.e("MainActivity", "Ошибка при отправке запроса: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d("MainActivity", "Ответ от сервера: $responseData")
                if (response.isSuccessful) {
                    Log.d("MainActivity", "Успешный ответ от сервера: $responseData")

                    // Извлечение токена из ответа
                    val jsonObject = JSONObject(responseData)
                    val token = jsonObject.getString("token")

                    // Сохранение токена в SharedPreferences
                    val sharedPreferences: SharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putString("token", token)
                        apply()
                    }

                    // Переход к HomeActivity
                    val intent = Intent(this@MainActivity, HomeActivity::class.java)
                    startActivity(intent)
                } else {
                    Log.e("MainActivity", "Ошибка на сервере: ${response.code}, ответ: $responseData")
                    runOnUiThread {
                        val layoutInflater = layoutInflater
                        val customToastView = layoutInflater.inflate(R.layout.custom_toast, null)
                        val toastText: TextView = customToastView.findViewById(R.id.toastText)
                        toastText.text = "Ошибка: ${response.message}"

                        val toast = Toast(applicationContext)
                        toast.duration = Toast.LENGTH_LONG
                        toast.view = customToastView
                        toast.show()
                    }
                }
            }
        })
    }
}
