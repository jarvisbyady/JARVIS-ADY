
package com.ady.jarvis

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var chat: LinearLayout
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private val micRequest = 101
    private val voiceRequest = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildScreen()
        addMessage("JARVIS", "Namaste Ady! Main JARVIS hoon. Bataiye, aaj kya karna hai?")
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 16)
            setBackgroundColor(0xFF080F19.toInt())
        }

        val header = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFF00D9FF.toInt())
            setPadding(0, 12, 0, 4)
        }

        val subtitle = TextView(this).apply {
            text = "YOUR PERSONAL AI ASSISTANT"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(0xFF91A7B8.toInt())
            setPadding(0, 0, 0, 20)
        }

        chat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        val scroll = ScrollView(this).apply {
            addView(chat)
        }

        val settings = Button(this).apply {
            text = "⚙ Settings"
            setOnClickListener { showSettings() }
        }

        input = EditText(this).apply {
            hint = "Ask JARVIS anything..."
            setHintTextColor(0xFF8294A5.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setSingleLine(false)
            setPadding(14, 12, 14, 12)
        }

        val mic = Button(this).apply {
            text = "🎙 Speak"
            setOnClickListener {
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                    startVoice()
                } else {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.RECORD_AUDIO),
                        micRequest
                    )
                }
            }
        }

        val send = Button(this).apply {
            text = "SEND ➤"
            setOnClickListener {
                val message = input.text.toString().trim()
                if (message.isNotEmpty()) {
                    input.text.clear()
                    addMessage("YOU", message)
                    askJarvis(message)
                }
            }
        }

        root.addView(header)
        root.addView(subtitle)
        root.addView(settings)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(input)
        root.addView(mic)
        root.addView(send)

        setContentView(root)
    }

    private fun addMessage(who: String, message: String) {
        val view = TextView(this).apply {
            text = "$who\n$message"
            textSize = 16f
            setTextColor(
                if (who == "JARVIS") 0xFF00D9FF.toInt()
                else 0xFFFFFFFF.toInt()
            )
            setPadding(12, 12, 12, 12)
        }
        chat.addView(view)
    }

    private fun askJarvis(message: String) {
        val worker = getSharedPreferences("jarvis", MODE_PRIVATE)
            .getString("worker_url", "") ?: ""

        if (worker.isBlank()) {
            addMessage("JARVIS", "Pehle Settings mein apna AI server URL set kijiye.")
            return
        }

        addMessage("JARVIS", "Soch raha hoon...")

        Thread {
            var answer = "Server se connect nahi ho paya. Dobara try kijiye."
            try {
                val connection = URL(worker.trimEnd('/') + "/chat")
                    .openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type", "application/json"
                )

                val body = JSONObject().put("message", message).toString()
                connection.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }

                val stream = if (connection.responseCode in 200..299)
                    connection.inputStream else connection.errorStream

                val response = stream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                answer = json.optString(
                    "reply",
                    json.optString("response", response)
                )
                connection.disconnect()
            } catch (e: Exception) {
                answer = "Connection error: ${e.message ?: "Unknown error"}"
            }

            runOnUiThread {
                addMessage("JARVIS", answer)
                if (!answer.startsWith("Connection error")) {
                    tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
                }
            }
        }.start()
    }

    private fun startVoice() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS is listening...")
            startActivityForResult(intent, voiceRequest)
        } catch (e: Exception) {
            addMessage("JARVIS", "Voice recognition abhi available nahi hai.")
        }
    }

    @Deprecated("Legacy voice result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == voiceRequest && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )?.firstOrNull()
            if (!result.isNullOrBlank()) {
                input.setText(result)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == micRequest &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startVoice()
        }
    }

    private fun showSettings() {
        val field = EditText(this).apply {
            hint = "https://your-worker.workers.dev"
            setText(
                getSharedPreferences("jarvis", MODE_PRIVATE)
                    .getString("worker_url", "")
            )
        }

        AlertDialog.Builder(this)
            .setTitle("JARVIS AI Server")
            .setMessage("Apna Cloudflare Worker URL enter karein.")
            .setView(field)
            .setPositiveButton("Save") { _, _ ->
                getSharedPreferences("jarvis", MODE_PRIVATE)
                    .edit()
                    .putString("worker_url", field.text.toString().trim())
                    .apply()
                addMessage("JARVIS", "Server URL saved.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("hi", "IN")
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }
}
