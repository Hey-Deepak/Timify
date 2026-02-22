package com.streamliners.timify.feature.voiceCapture

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.streamliners.timify.feature.voice.sarvam.SarvamSTTService
import com.streamliners.timify.feature.voiceCapture.VoiceCaptureViewModel.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceCaptureScreen(
    navController: NavController,
    viewModel: VoiceCaptureViewModel
) {
    val scope = rememberCoroutineScope()
    val sttService: SarvamSTTService = koinInject()

    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var recordingBuffer by remember { mutableStateOf(ByteArrayOutputStream()) }

    DisposableEffect(Unit) {
        onDispose {
            audioRecord?.release()
        }
    }

    Scaffold { innerPadding ->
        AnimatedContent(
            targetState = viewModel.state.value,
            label = "VoiceState",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { currentState ->
            when (currentState) {
                State.IDLE -> {
                    IdleView(
                        onStartRecording = {
                            if (!micPermission.status.isGranted) {
                                micPermission.launchPermissionRequest()
                                return@IdleView
                            }
                            viewModel.state.value = State.LISTENING
                            scope.launch {
                                val (recorder, buffer) = startRecording()
                                audioRecord = recorder
                                recordingBuffer = buffer
                            }
                        }
                    )
                }

                State.LISTENING -> {
                    ListeningView(
                        onStopRecording = {
                            scope.launch {
                                viewModel.state.value = State.PROCESSING
                                val recorder = audioRecord ?: return@launch
                                val pcmBytes = stopAndGetAudio(recorder, recordingBuffer)
                                audioRecord = null

                                if (pcmBytes.isEmpty()) {
                                    viewModel.state.value = State.IDLE
                                    return@launch
                                }

                                val wavBytes = encodeToWav(pcmBytes, 16000, 1, 16)
                                try {
                                    val transcript = sttService.transcribeBytes(
                                        audioBytes = wavBytes,
                                        fileName = "recording.wav",
                                        languageCode = "hi-IN"
                                    )
                                    if (transcript.isBlank()) {
                                        viewModel.state.value = State.IDLE
                                    } else {
                                        viewModel.processTranscription(transcript)
                                    }
                                } catch (e: Exception) {
                                    viewModel.state.value = State.IDLE
                                }
                            }
                        }
                    )
                }

                State.PROCESSING -> {
                    ProcessingView()
                }

                State.CONFIRMING -> {
                    ConfirmingView(
                        transcribedText = viewModel.transcribedText.value,
                        tasks = viewModel.extractedTasks,
                        onUpdateTask = viewModel::updateTask,
                        onRemoveTask = viewModel::removeTask,
                        onConfirm = viewModel::confirmAndSave,
                        onCancel = viewModel::reset
                    )
                }

                State.SAVED -> {
                    SavedView(
                        taskCount = viewModel.extractedTasks.size,
                        onRecordMore = viewModel::reset,
                        onGoHome = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleView(onStartRecording: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Voice Capture",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tell me about your day in Hindi.\nI'll extract your tasks automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        LargeFloatingActionButton(
            onClick = onStartRecording,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                Icons.Default.KeyboardVoice,
                contentDescription = "Start Recording",
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tap to start",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ListeningView(onStopRecording: () -> Unit) {
    val pulseColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.error,
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = pulseColor
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Speak about your activities in Hindi",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        LargeFloatingActionButton(
            onClick = onStopRecording,
            containerColor = MaterialTheme.colorScheme.error
        ) {
            Icon(
                Icons.Default.Stop,
                contentDescription = "Stop Recording",
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tap to stop",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProcessingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Processing...",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Transcribing your voice and extracting tasks",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConfirmingView(
    transcribedText: String,
    tasks: List<VoiceCaptureViewModel.EditableTask>,
    onUpdateTask: (Int, VoiceCaptureViewModel.EditableTask) -> Unit,
    onRemoveTask: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var editingIndex by remember { mutableStateOf(-1) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Transcription preview
        item {
            Text(
                text = "What I heard:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = transcribedText,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Extracted Tasks (${tasks.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Task cards
        itemsIndexed(tasks) { index, task ->
            TaskConfirmationCard(
                task = task,
                isEditing = editingIndex == index,
                onEdit = { editingIndex = index },
                onSaveEdit = { updated ->
                    onUpdateTask(index, updated)
                    editingIndex = -1
                },
                onCancelEdit = { editingIndex = -1 },
                onRemove = { onRemoveTask(index) }
            )
        }

        // Confirm / Cancel buttons
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Discard")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    enabled = tasks.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save ${tasks.size} tasks")
                }
            }
        }
    }
}

@Composable
private fun TaskConfirmationCard(
    task: VoiceCaptureViewModel.EditableTask,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onSaveEdit: (VoiceCaptureViewModel.EditableTask) -> Unit,
    onCancelEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        if (isEditing) {
            EditTaskForm(task, onSaveEdit, onCancelEdit)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${task.startTime} - ${task.endTime} (${task.durationInMins}m)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.category.isNotEmpty()) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete, "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EditTaskForm(
    task: VoiceCaptureViewModel.EditableTask,
    onSave: (VoiceCaptureViewModel.EditableTask) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(task.name) }
    var startTime by remember { mutableStateOf(task.startTime) }
    var endTime by remember { mutableStateOf(task.endTime) }
    var category by remember { mutableStateOf(task.category) }

    Column(modifier = Modifier.padding(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Task Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                label = { Text("Start") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = endTime,
                onValueChange = { endTime = it },
                label = { Text("End") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Button(onClick = {
                onSave(
                    task.copy(
                        name = name,
                        startTime = startTime,
                        endTime = endTime,
                        category = category
                    )
                )
            }) { Text("Save") }
        }
    }
}

@Composable
private fun SavedView(
    taskCount: Int,
    onRecordMore: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "$taskCount tasks saved!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onRecordMore) {
                Icon(Icons.Default.KeyboardVoice, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Record More")
            }
            Button(onClick = onGoHome) {
                Text("Go to Home")
            }
        }
    }
}

// Audio recording helpers

private const val SAMPLE_RATE = 16000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

private suspend fun startRecording(): Pair<AudioRecord, ByteArrayOutputStream> = withContext(Dispatchers.IO) {
    val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    val recorder = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
        bufferSize
    )
    val buffer = ByteArrayOutputStream()
    recorder.startRecording()

    val readBuffer = ByteArray(bufferSize)
    Thread {
        while (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val bytesRead = recorder.read(readBuffer, 0, readBuffer.size)
            if (bytesRead > 0) {
                buffer.write(readBuffer, 0, bytesRead)
            }
        }
    }.start()

    Pair(recorder, buffer)
}

private suspend fun stopAndGetAudio(
    recorder: AudioRecord,
    buffer: ByteArrayOutputStream
): ByteArray = withContext(Dispatchers.IO) {
    recorder.stop()
    recorder.release()
    Thread.sleep(100)
    buffer.toByteArray()
}

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

    dos.writeBytes("RIFF")
    dos.writeInt(Integer.reverseBytes(totalSize))
    dos.writeBytes("WAVE")
    dos.writeBytes("fmt ")
    dos.writeInt(Integer.reverseBytes(16))
    dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
    dos.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
    dos.writeInt(Integer.reverseBytes(sampleRate))
    dos.writeInt(Integer.reverseBytes(byteRate))
    dos.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt())
    dos.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
    dos.writeBytes("data")
    dos.writeInt(Integer.reverseBytes(dataSize))
    dos.write(pcmData)

    return output.toByteArray()
}
