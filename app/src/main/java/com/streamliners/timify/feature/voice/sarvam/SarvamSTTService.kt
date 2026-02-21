package com.streamliners.timify.feature.voice.sarvam

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Sarvam AI Speech-to-Text service using Saaras v3 model.
 * Supports Hindi and 22+ Indian languages with auto language detection.
 * Handles code-mixed audio (Hindi + English) seamlessly.
 *
 * API: POST https://api.sarvam.ai/speech-to-text
 * Auth: api-subscription-key header
 * Content-Type: multipart/form-data
 * Supports: WAV, MP3, AAC, OGG, OPUS, FLAC, M4A, AMR, WebM
 */
class SarvamSTTService(
    private val httpClient: HttpClient,
    private val apiKey: String
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Transcribe an audio file to text using Sarvam's Saaras v3 model.
     * @param audioFile The audio file to transcribe (max 30 seconds for REST API)
     * @param languageCode BCP-47 code, or "unknown" for auto-detection (default: "hi-IN")
     * @return Transcribed text
     */
    suspend fun transcribe(
        audioFile: File,
        languageCode: String = "hi-IN"
    ): String {
        val response = httpClient.submitFormWithBinaryData(
            url = "https://api.sarvam.ai/speech-to-text",
            formData = formData {
                append("file", audioFile.readBytes(), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
                    append(HttpHeaders.ContentType, audioContentType(audioFile))
                })
                append("model", "saaras:v3")
                append("language_code", languageCode)
            }
        ) {
            header("api-subscription-key", apiKey)
        }

        val responseBody = response.bodyAsText()
        val sttResponse = json.decodeFromString(SttResponse.serializer(), responseBody)

        return sttResponse.transcript
    }

    /**
     * Transcribe audio bytes directly (useful when recording from microphone).
     * @param audioBytes Raw audio data
     * @param fileName File name with extension for content type detection
     * @param languageCode BCP-47 code, or "unknown" for auto-detection
     * @return Transcribed text
     */
    suspend fun transcribeBytes(
        audioBytes: ByteArray,
        fileName: String = "recording.wav",
        languageCode: String = "hi-IN"
    ): String {
        val response = httpClient.submitFormWithBinaryData(
            url = "https://api.sarvam.ai/speech-to-text",
            formData = formData {
                append("file", audioBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    append(HttpHeaders.ContentType, audioContentType(fileName))
                })
                append("model", "saaras:v3")
                append("language_code", languageCode)
            }
        ) {
            header("api-subscription-key", apiKey)
        }

        val responseBody = response.bodyAsText()
        val sttResponse = json.decodeFromString(SttResponse.serializer(), responseBody)

        return sttResponse.transcript
    }

    private fun audioContentType(file: File): String = audioContentType(file.name)

    private fun audioContentType(fileName: String): String {
        return when {
            fileName.endsWith(".wav") -> "audio/wav"
            fileName.endsWith(".mp3") -> "audio/mpeg"
            fileName.endsWith(".aac") -> "audio/aac"
            fileName.endsWith(".ogg") -> "audio/ogg"
            fileName.endsWith(".opus") -> "audio/opus"
            fileName.endsWith(".flac") -> "audio/flac"
            fileName.endsWith(".m4a") -> "audio/mp4"
            fileName.endsWith(".amr") -> "audio/amr"
            fileName.endsWith(".webm") -> "audio/webm"
            else -> "audio/wav"
        }
    }

    @Serializable
    private data class SttResponse(
        val transcript: String,
        val language_code: String = ""
    )
}
