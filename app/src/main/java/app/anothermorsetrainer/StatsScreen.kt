package app.anothermorsetrainer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.anothermorsetrainer.morsekit.SessionRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/**
 * Your progress at a glance: the daily practice streak, lifetime totals, your
 * best recognition time, and a list of recent sessions. Reads the persisted
 * [Stats] singleton (no work to do here but render).
 */
@Composable
fun StatsScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(8.dp)) { Text("‹ Back") }

      CenteredContent {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your Progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            if (Stats.totalSessions == 0) {
                Spacer(Modifier.height(40.dp))
                Text(
                    "No sessions yet.\nFinish a practice round and your stats will show up here.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                return@Column
            }

            // Streak banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔥 ${Stats.currentStreak}", fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (Stats.currentStreak == 1) "day streak" else "day streak",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Longest: ${Stats.longestStreak} days",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Lifetime tiles
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Sessions", "${Stats.totalSessions}", Modifier.weight(1f))
                StatTile("Drills", "${Stats.totalAttempts}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Accuracy", "${(Stats.overallAccuracy * 100).roundToInt()}%", Modifier.weight(1f))
                StatTile("Best copy", Stats.bestTtrMs?.let { fmtMs(it) } ?: "—", Modifier.weight(1f))
            }

            val charRows = Stats.charStats.entries
                .mapNotNull { e -> e.value.medianMs?.let { CharBar(e.key, it, e.value.accuracy) } }
                .sortedWith(compareBy(SessionRecord.characterOrder) { it.character })
            if (charRows.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Recognition speed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Median time to copy each character — shorter is better.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                val axisMax = SessionRecord.axisCeilingMS(charRows.maxOf { it.medianMs })
                charRows.forEach { row ->
                    RecognitionBar(row = row, axisMaxMs = axisMax)
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Recent sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Stats.recent.forEach { s ->
                SessionRow(
                    mode = s.mode,
                    date = LocalDate.ofEpochDay(s.epochDay).format(DATE_FMT),
                    detail = "${s.correct}/${s.attempts} · ${(s.accuracy * 100).roundToInt()}%"
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
      }
    }
}

/** One row of the recognition chart: a character, its median copy time, and accuracy. */
private data class CharBar(val character: String, val medianMs: Int, val accuracy: Double)

@Composable
private fun RecognitionBar(row: CharBar, axisMaxMs: Int) {
    val fraction = (row.medianMs.toFloat() / axisMaxMs).coerceIn(0.04f, 1f)
    val barColor = when {
        row.accuracy >= 0.9 -> MaterialTheme.colorScheme.primary
        row.accuracy >= 0.7 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            row.character,
            modifier = Modifier.width(28.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(barColor)
            )
        }
        Text(
            fmtMs(row.medianMs),
            modifier = Modifier.width(52.dp).padding(start = 8.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SessionRow(mode: String, date: String, detail: String) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(mode, fontWeight = FontWeight.SemiBold)
                Text(date, style = MaterialTheme.typography.labelSmall)
            }
            Text(detail, modifier = Modifier.align(Alignment.CenterEnd), fontWeight = FontWeight.Medium)
        }
    }
}

private fun fmtMs(ms: Int): String =
    if (ms >= 1000) "%.1fs".format(ms / 1000.0) else "${ms}ms"
