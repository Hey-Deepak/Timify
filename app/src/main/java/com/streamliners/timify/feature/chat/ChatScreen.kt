package com.streamliners.timify.feature.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.streamliners.base.ext.showFailureMessage
import com.streamliners.base.taskState.comp.whenLoaded
import com.streamliners.compose.android.comp.appBar.TitleBarScaffold
import com.streamliners.compose.comp.select.Layout
import com.streamliners.compose.comp.select.RadioGroup
import com.streamliners.timify.feature.chat.ChatViewModel.Mode.Text
import com.streamliners.timify.feature.chat.comp.MessagesList
import com.streamliners.timify.feature.chat.comp.TextInput
import com.streamliners.timify.feature.chat.comp.VoiceMode
import com.streamliners.timify.feature.chat.viewModelExt.ENABLE_INSIGHTS_CHAT
import com.streamliners.timify.feature.voice.SpeechRecognitionButton
import com.streamliners.timify.feature.voice.sarvam.SarvamSpeechRecognitionButton
import com.streamliners.timify.feature.voice.sarvam.SarvamSTTService
import org.koin.compose.koinInject
import com.streamliners.timify.ui.main.Screen

@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel
) {
    val prompt = rememberSaveable { mutableStateOf("") }

    LaunchedEffect(key1 = Unit) {
        viewModel.loadChat()
    }

    TitleBarScaffold(
        title = "Timify",
        actions = {
            IconButton(
                onClick = {
                    viewModel.saveTaskInfoToLocal {
                        navController.navigate(Screen.SheetSync.route)
                    }
                }
            ) {
                Icon(imageVector = Icons.Default.TableChart, contentDescription = "Sheet Sync")
            }

            IconButton(
                onClick = {
                    viewModel.saveTaskInfoToLocal {
                        viewModel.isNewChatHappened.value = false
                        navController.navigate(Screen.PieChart.route)
                    }

                }
            ) {
                Icon(imageVector = Icons.Default.Insights, contentDescription = "Insights")
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {

            if (ENABLE_INSIGHTS_CHAT) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    RadioGroup(
                        selection = viewModel.type.value,
                        onSelectionChange = {
                            viewModel.type.value = it
                            viewModel.loadChat()
                        },
                        options = ChatViewModel.ChatType.entries.toList(),
                        labelExtractor = { it.name },
                        layout = Layout.Row
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                viewModel.data.whenLoaded { data ->
                    MessagesList(data)
                }
            }

            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sarvam AI Hindi voice input
                val sttService: SarvamSTTService = koinInject()
                SarvamSpeechRecognitionButton(
                    sttService = sttService,
                    onInput = { input, nextInput ->
                        prompt.value = input
                        viewModel.mode.value = ChatViewModel.Mode.Voice
                        viewModel.sendPrompt(
                            prompt = prompt.value,
                            onSuccess = { prompt.value = "" },
                            takeNextVoicePrompt = nextInput
                        )
                    },
                    showError = viewModel::showFailureMessage,
                    onDismiss = { viewModel.mode.value = Text }
                )

                AnimatedContent(
                    modifier = Modifier.weight(1f),
                    targetState = viewModel.mode,
                    label = "Mode"
                ) { mode ->

                    if (viewModel.mode.value == Text) {
                        TextInput(prompt, viewModel)
                    } else {
                        VoiceMode(
                            modifier = Modifier.weight(1f),
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}