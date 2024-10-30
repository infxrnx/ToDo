package com.example.guessgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType

class SignupActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val signupButton: Button = findViewById(R.id.createButton)
        val escapeButton: Button = findViewById(R.id.escapeButton)
        val usernameEditText: EditText = findViewById(R.id.usernameText2)
        val passwordEditText: EditText = findViewById(R.id.passwordText2)

        signupButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            Log.d("SignupActivity", "Логин: $username, Пароль: $password")
            sendSignupRequest(username, password)
        }

        escapeButton.setOnClickListener {
            // Переход на новую активность SignupActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun sendSignupRequest(username: String, password: String) {
        val url = Constants.BASE_URL + Constants.REGISTER_URL // URL для регистрации
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

        Log.d("SignupActivity", "Запрос отправляется на сервер: $url")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Логирование ошибки
                Log.e("SignupActivity", "Ошибка при отправке запроса: ${e.message}")
                runOnUiThread {
                    showToast("Ошибка при регистрации: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d("SignupActivity", "Ответ от сервера: $responseData")
                if (response.isSuccessful) {
                    Log.d("SignupActivity", "Успешный ответ от сервера: $responseData")
                    runOnUiThread {
                        Toast.makeText(this@SignupActivity, "Регистрация успешна!", Toast.LENGTH_LONG).show()
                        // Переход на активность входа
                        val intent = Intent(this@SignupActivity, MainActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    Log.e("SignupActivity", "Ошибка на сервере: ${response.code}, ответ: $responseData")
                    runOnUiThread {
                        showToast("Ошибка: ${response.message}")
                    }
                }
            }
        })
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
    }
}
