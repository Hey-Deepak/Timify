package com.streamliners.timify.feature.voice.sarvam

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Speech recognition button that uses Sarvam AI's Saaras v3 model
 * for Hindi and Indian language speech-to-text.
 *
 * Records audio from the microphone, sends it to Sarvam STT API,
 * and returns the transcribed text.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SarvamSpeechRecognitionButton(
    modifier: Modifier = Modifier,
    sttService: SarvamSTTService,
    onInput: (input: String, nextInput: () -> Unit) -> Unit,
    showError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }

    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    DisposableEffect(Unit) {
        onDispose {
            audioRecord?.release()
        }
    }

    FilledTonalIconButton(
        modifier = modifier,
        onClick = {
            if (!micPermission.status.isGranted) {
                micPermission.launchPermissionRequest()
                return@FilledTonalIconButton
            }

            if (isRecording) {
                // Stop recording and transcribe
                isRecording = false
                val recorder = audioRecord ?: return@FilledTonalIconButton

                scope.launch {
                    try {
                        val audioBytes = stopAndGetAudio(recorder)
                        audioRecord = null

                        if (audioBytes.isEmpty()) {
                            showError("No audio recorded")
                            onDismiss()
                            return@launch
                        }

                        val wavBytes = encodeToWav(audioBytes, SAMPLE_RATE, 1, 16)
                        val transcript = sttService.transcribeBytes(
                            audioBytes = wavBytes,
                            fileName = "recording.wav",
                            languageCode = "hi-IN"
                        )

                        if (transcript.isBlank()) {
                            showError("Could not recognize speech")
                            onDismiss()
                        } else {
                            onInput(transcript) {
                                // For next voice input, user taps button again
                            }
                        }
                    } catch (e: Exception) {
                        showError("Speech recognition failed: ${e.message}")
                        onDismiss()
                    }
                }
            } else {
                // Start recording
                scope.launch {
                    try {
                        audioRecord = startRecording()
                        isRecording = true
                    } catch (e: Exception) {
                        showError("Failed to start recording: ${e.message}")
                    }
                }
            }
        }
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.KeyboardVoice,
            contentDescription = if (isRecording) "Stop Recording" else "Voice Input (Hindi)"
        )
    }
}

private const val SAMPLE_RATE = 16000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

private var recordingBuffer = ByteArrayOutputStream()

private suspend fun startRecording(): AudioRecord = withContext(Dispatchers.IO) {
    val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    val recorder = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
        bufferSize
    )

    recordingBuffer = ByteArrayOutputStream()
    recorder.startRecording()

    // Read audio data in background
    val buffer = ByteArray(bufferSize)
    Thread {
        while (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val bytesRead = recorder.read(buffer, 0, buffer.size)
            if (bytesRead > 0) {
                recordingBuffer.write(buffer, 0, bytesRead)
            }
        }
    }.start()

    recorder
}

private suspend fun stopAndGetAudio(recorder: AudioRecord): ByteArray = withContext(Dispatchers.IO) {
    recorder.stop()
    recorder.release()
    // Small delay to let the reading thread finish
    Thread.sleep(100)
    recordingBuffer.toByteArray()
}

/**
 * Encode raw PCM audio bytes into a WAV file format.
 */
private fun encodeToWav(
    pcmData: ByteArray,
    sampleRate: Int,
    channels: Int,
    bitsPerSample: Int
): ByteArray {
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    val dataSize = pcmData.size
    val totalSize = 36 + dataSize

    val output = ByteArrayOutputStream()
    val dos = DataOutputStream(output)

    // WAV header
    dos.writeBytes("RIFF")
    dos.writeInt(Integer.reverseBytes(totalSize))
    dos.writeBytes("WAVE")

    // fmt chunk
    dos.writeBytes("fmt ")
    dos.writeInt(Integer.reverseBytes(16)) // chunk size
    dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // PCM format
    dos.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
    dos.writeInt(Integer.reverseBytes(sampleRate))
    dos.writeInt(Integer.reverseBytes(byteRate))
    dos.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt())
    dos.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())

    // data chunk
    dos.writeBytes("data")
    dos.writeInt(Integer.reverseBytes(dataSize))
    dos.write(pcmData)

    return output.toByteArray()
}
