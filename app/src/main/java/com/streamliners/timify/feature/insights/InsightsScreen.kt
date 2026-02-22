package com.streamliners.timify.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: InsightsViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.loadInsights()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
        }

        // Ask a question
        item {
            OutlinedTextField(
                value = viewModel.customQuestion.value,
                onValueChange = { viewModel.customQuestion.value = it },
                label = { Text("Ask about your time...") },
                placeholder = { Text("e.g., How much time did I spend on work this week?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.askQuestion(viewModel.customQuestion.value) },
                        enabled = viewModel.customQuestion.value.isNotBlank() && !viewModel.isLoading.value
                    ) {
                        Icon(Icons.Default.Send, "Ask")
                    }
                }
            )
        }

        if (viewModel.isLoading.value) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.padding(8.dp))
                    Text("Analyzing your data...")
                }
            }
        }

        items(viewModel.insights) { insight ->
            InsightCard(insight)
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun InsightCard(insight: InsightsViewModel.Insight) {
    val containerColor = when (insight.type) {
        InsightsViewModel.InsightType.INFO -> MaterialTheme.colorScheme.surfaceVariant
        InsightsViewModel.InsightType.TIP -> MaterialTheme.colorScheme.primaryContainer
        InsightsViewModel.InsightType.ALERT -> MaterialTheme.colorScheme.errorContainer
    }

    val icon = when (insight.type) {
        InsightsViewModel.InsightType.INFO -> Icons.Default.Info
        InsightsViewModel.InsightType.TIP -> Icons.Default.AutoAwesome
        InsightsViewModel.InsightType.ALERT -> Icons.Default.Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.padding(4.dp))
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = insight.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
