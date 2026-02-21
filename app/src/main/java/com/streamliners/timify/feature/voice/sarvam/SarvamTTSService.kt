package com.streamliners.timify.feature.voice.sarvam

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Sarvam AI Text-to-Speech service using Bulbul v3 model.
 * Supports Hindi and 10+ Indian languages with natural-sounding voices.
 *
 * API: POST https://api.sarvam.ai/text-to-speech
 * Auth: api-subscription-key header
 * Response: base64-encoded WAV audio
 */
class SarvamTTSService(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val cacheDir: File
) {

    private var mediaPlayer: MediaPlayer? = null

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Convert text to speech and play it.
     * @param text The text to speak (Hindi or code-mixed)
     * @param languageCode BCP-47 language code (default: "hi-IN" for Hindi)
     * @param speaker Voice name (default: "Shubh")
     */
    suspend fun speak(
        text: String,
        languageCode: String = "hi-IN",
        speaker: String = "Shubh"
    ) {
        val audioBase64 = textToSpeech(text, languageCode, speaker)
        playBase64Audio(audioBase64)
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
    }

    private suspend fun textToSpeech(
        text: String,
        languageCode: String,
        speaker: String
    ): String {
        val requestBody = TtsRequest(
            inputs = listOf(text),
            target_language_code = languageCode,
            speaker = speaker,
            model = "bulbul:v3"
        )

        val response = httpClient.post("https://api.sarvam.ai/text-to-speech") {
            contentType(ContentType.Application.Json)
            header("api-subscription-key", apiKey)
            setBody(json.encodeToString(TtsRequest.serializer(), requestBody))
        }

        val responseBody = response.bodyAsText()
        val ttsResponse = json.decodeFromString(TtsResponse.serializer(), responseBody)

        return ttsResponse.audios.firstOrNull()
            ?: error("No audio returned from Sarvam TTS API")
    }

    private suspend fun playBase64Audio(base64Audio: String) {
        stop()

        val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
        val tempFile = File(cacheDir, "sarvam_tts_temp.wav")
        FileOutputStream(tempFile).use { it.write(audioBytes) }

        suspendCoroutine { cont ->
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    cont.resume(Unit)
                    tempFile.delete()
                }
                setOnErrorListener { _, what, extra ->
                    cont.resumeWithException(
                        RuntimeException("MediaPlayer error: what=$what extra=$extra")
                    )
                    true
                }
                prepare()
                start()
            }
        }
    }

    @Serializable
    private data class TtsRequest(
        val inputs: List<String>,
        val target_language_code: String,
        val speaker: String,
        val model: String
    )

    @Serializable
    private data class TtsResponse(
        val audios: List<String>
    )
}
