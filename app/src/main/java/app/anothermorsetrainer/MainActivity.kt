package app.anothermorsetrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.anothermorsetrainer.morsekit.MorseData
import app.anothermorsetrainer.morsekit.PhraseQuiz
import app.anothermorsetrainer.morsekit.ProgressiveCharacters
import app.anothermorsetrainer.morsekit.QuizSource
import app.anothermorsetrainer.morsekit.TrainerEngine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.init(this)
        Stats.init(this)
        setContent {
            AmtTheme {
                AppBackground {
                    AppRoot()
                }
            }
        }
    }
}

/** A selectable training mode, each backed by a ported [QuizSource]. */
data class QuizMode(val title: String, val subtitle: String, val make: () -> QuizSource)

/** The menu of modes the home screen offers — every one drives the same QuizScreen. */
val QUIZ_MODES: List<QuizMode> = listOf(
    QuizMode("Characters", "Koch-method ladder, A–Z 0–9") {
        ProgressiveCharacters(TrainerEngine(TrainerEngine.Config(wpm = Settings.characterWpm)))
    },
    QuizMode("Common Words", "Hear the word, pick the word") {
        PhraseQuiz("Words", MorseData.wordItems)
    },
    QuizMode("Abbreviations", "CW shorthand → meaning") {
        PhraseQuiz("Abbreviations", MorseData.abbreviationItems)
    },
    QuizMode("Q-Codes", "QRZ, QSY, QTH …") {
        PhraseQuiz("Q-Codes", MorseData.qCodeItems)
    }
)

private sealed interface Route {
    data object Home : Route
    data class Quiz(val mode: QuizMode) : Route
    data object Pileup : Route
    data object Exam : Route
    data object Listen : Route
    data object Settings : Route
    data object Stats : Route
}

@Composable
private fun AppRoot() {
    var route by remember { mutableStateOf<Route>(Route.Home) }
    when (val r = route) {
        Route.Home -> HomeScreen(
            onPickQuiz = { route = Route.Quiz(it) },
            onPickPileup = { route = Route.Pileup },
            onPickExam = { route = Route.Exam },
            onPickListen = { route = Route.Listen },
            onPickSettings = { route = Route.Settings },
            onPickStats = { route = Route.Stats }
        )
        is Route.Quiz -> QuizScreen(
            title = r.mode.title,
            onBack = { route = Route.Home },
            makeSource = r.mode.make
        )
        Route.Pileup -> PileupScreen(onBack = { route = Route.Home })
        Route.Exam -> CodeExamScreen(onBack = { route = Route.Home })
        Route.Listen -> ListenScreen(onBack = { route = Route.Home })
        Route.Settings -> SettingsScreen(onBack = { route = Route.Home })
        Route.Stats -> StatsScreen(onBack = { route = Route.Home })
    }
}

/** Subtitle for the pileup menu card. */
const val PILEUP_SUBTITLE = "Call CQ and work a CW pileup"
