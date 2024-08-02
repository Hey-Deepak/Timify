package com.streamliners.timify.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.ai.client.generativeai.type.asTextOrNull
import com.streamliners.compose.android.comp.appBar.TitleBarScaffold

@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel
) {


    val placeholderPrompt = ""
    val placeholderResult = "Rusult Yaha aayega..."
    var prompt by rememberSaveable { mutableStateOf(placeholderPrompt) }
    var result by rememberSaveable { mutableStateOf(placeholderResult) }
    val uiState by viewModel.uiState.collectAsState()


    TitleBarScaffold(
        title = "Timify",
        navigateUp = { navController.navigateUp() }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {


            if (uiState is UiState.Loading) {
                Column (
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ){
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))

                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var textColor = MaterialTheme.colorScheme.onSurface
                    if (uiState is UiState.Error) {
                        textColor = MaterialTheme.colorScheme.error
                        result = (uiState as UiState.Error).errorMessage
                    } else if (uiState is UiState.Success) {
                        textColor = MaterialTheme.colorScheme.onSurface
                        result = (uiState as UiState.Success).outputText
                    }

                    viewModel.chatHistoryState.forEach { chat ->
                        Text(
                            text = chat.parts[0].asTextOrNull().toString(),
                            textAlign = TextAlign.Start,
                            color = textColor,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                                .wrapContentSize()

                        )
                    }

                }
            }

            Column {
                Row(
                    modifier = Modifier
                        .padding(all = 8.dp)
                ) {
                    TextField(
                        value = prompt,
                        label = { Text("Prompt") },
                        onValueChange = { prompt = it },
                        modifier = Modifier
                            .weight(0.8f)
                            .padding(end = 16.dp)
                            .align(Alignment.CenterVertically)
                    )

                    Button(
                        onClick = {
                            viewModel.sendPrompt(prompt)
                            prompt = ""
                        },
                        enabled = prompt.isNotEmpty(),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(text = "Go")
                    }
                }
            }


        }

    }


}

