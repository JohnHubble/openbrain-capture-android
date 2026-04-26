package com.hubble.openbrain.ui.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hubble.openbrain.data.db.Thought
import com.hubble.openbrain.data.db.ThoughtStatus
import com.hubble.openbrain.ui.theme.LocalOpenBrainStrings
import com.hubble.openbrain.ui.theme.LocalOpenBrainTokens
import com.hubble.openbrain.ui.theme.ThemedPageTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val thoughts by viewModel.thoughts.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val total = counts.values.sum()

    val strings = LocalOpenBrainStrings.current
    Column(Modifier.fillMaxSize()) {
        ThemedPageTitle(
            tab = strings.historyTab,
            subtitle = if (total == 0) "No thoughts yet" else "$total thoughts captured",
        )

        FilterChipRow(
            selected = filter,
            counts = counts,
            onSelect = viewModel::setFilter,
        )

        val failedCount = counts[ThoughtStatus.FAILED] ?: 0
        if (failedCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                AssistChip(
                    onClick = viewModel::retryFailed,
                    label = { Text("Retry failed ($failedCount)") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
            }
        }

        if (thoughts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Start capturing to fill this list",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = thoughts, key = { it.id }) { thought ->
                    ThoughtRow(thought = thought)
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    selected: ThoughtStatus?,
    counts: Map<ThoughtStatus, Int>,
    onSelect: (ThoughtStatus?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipItem(
            label = "All",
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        FilterChipItem(
            label = "Sent (${counts[ThoughtStatus.SENT] ?: 0})",
            selected = selected == ThoughtStatus.SENT,
            onClick = { onSelect(ThoughtStatus.SENT) },
        )
        FilterChipItem(
            label = "Pending (${counts[ThoughtStatus.PENDING] ?: 0})",
            selected = selected == ThoughtStatus.PENDING,
            onClick = { onSelect(ThoughtStatus.PENDING) },
        )
        FilterChipItem(
            label = "Failed (${counts[ThoughtStatus.FAILED] ?: 0})",
            selected = selected == ThoughtStatus.FAILED,
            onClick = { onSelect(ThoughtStatus.FAILED) },
        )
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

@Composable
private fun ThoughtRow(thought: Thought) {
    var expanded by rememberSaveable(thought.id) { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.RecordVoiceOver,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = thought.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = formatMeta(thought.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            StatusBadge(thought.status)
        }
    }
}

@Composable
private fun StatusBadge(status: ThoughtStatus) {
    val tokens = LocalOpenBrainTokens.current.extras
    val (bg: Color, fg: Color, label: String) = when (status) {
        ThoughtStatus.SENT -> Triple(tokens.successContainer, tokens.success, "Sent")
        ThoughtStatus.PENDING -> Triple(tokens.warningContainer, tokens.warning, "Pending")
        ThoughtStatus.FAILED -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "Failed")
    }
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bg,
    ) {
        Text(
            text = label.uppercase(Locale.US),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.W500,
            letterSpacing = 0.3.sp,
            color = fg,
        )
    }
}

private val META_FMT = SimpleDateFormat("MMM d · HH:mm", Locale.US)

private fun formatMeta(epochMs: Long): String = META_FMT.format(Date(epochMs))
