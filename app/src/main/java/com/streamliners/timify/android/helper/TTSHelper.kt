package com.streamliners.timify.android.helper

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.*
import android.speech.tts.UtteranceProgressListener
import com.streamliners.base.exception.BusinessException
import com.streamliners.timify.feature.voice.sarvam.SarvamTTSService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Text-to-Speech helper that supports:
 * - Sarvam AI Bulbul v3 for Hindi (primary)
 * - Android built-in TTS for English (fallback)
 */
class TTSHelper(
    private val context: Context,
    private val sarvamTTS: SarvamTTSService? = null
) {

    private var tts: TextToSpeech? = null

    /**
     * Speak text using the appropriate TTS engine.
     * Uses Sarvam for Hindi, Android TTS for English.
     */
    suspend fun speak(text: String, useHindi: Boolean = false) {
        if (useHindi && sarvamTTS != null) {
            sarvamTTS.speak(text)
        } else {
            speakWithAndroidTTS(text)
        }
    }

    fun stop() {
        tts?.stop()
        sarvamTTS?.stop()
    }

    fun shutdown() {
        tts?.shutdown(); tts = null
        sarvamTTS?.shutdown()
    }

    private suspend fun speakWithAndroidTTS(text: String) {
        if (tts == null) {
            init(context, text)
        } else {
            speakText(text)
        }
    }

    private suspend fun init(context: Context, text: String) {
        return suspendCoroutine { cont ->
            tts = TextToSpeech(context) { status ->
                if (status == SUCCESS) {
                    val result: Int = tts!!.setLanguage(Locale.US)
                    if (result != LANG_MISSING_DATA && result != LANG_NOT_SUPPORTED) {
                        CoroutineScope(cont.context).launch {
                            speakText(text)
                            cont.resume(Unit)
                        }
                    } else {
                        error("TTS Error Code : $result")
                    }
                } else {
                    error("TTS Init Error")
                }
            }
        }
    }

    private suspend fun speakText(text: String) {
        return suspendCoroutine { cont ->
            tts?.speak(text, QUEUE_FLUSH, null, "1")
            tts?.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onDone(utteranceId: String?) {
                        cont.resume(Unit)
                    }
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {
                        cont.resumeWithException(BusinessException("TTS Error"))
                    }
                }
            )
        }
    }

}
