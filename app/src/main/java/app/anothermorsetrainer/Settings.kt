package app.anothermorsetrainer

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.anothermorsetrainer.morsekit.MorseTiming

/**
 * App-wide, persisted preferences. A process-wide singleton (rather than a
 * threaded-through object) so any Composable can read it reactively and any
 * helper — e.g. [Haptics] — can consult it without plumbing.
 *
 * Backed by [SharedPreferences]; each property mirrors a stored key and is also
 * a Compose [mutableStateOf], so writing it both persists and recomposes
 * readers. The iOS app keeps the equivalent in `@AppStorage`/UserDefaults.
 */
object Settings {
    private lateinit var prefs: SharedPreferences

    // Sensible defaults that match what the screens used before settings existed.
    var characterWpm by mutableStateOf(18.0)
        private set
    var effectiveWpm by mutableStateOf(18.0)   // Farnsworth target; == character ⇒ standard timing
        private set
    var sidetoneHz by mutableStateOf(600.0)
        private set
    var hapticsEnabled by mutableStateOf(true)
        private set

    // Speak your answer instead of tapping (uses the microphone).
    var voiceAnswersEnabled by mutableStateOf(false)
        private set

    // Daily practice reminder (a notification to keep the streak alive).
    var remindersEnabled by mutableStateOf(false)
        private set
    var reminderHour by mutableStateOf(19)     // 7pm default
        private set
    var reminderMinute by mutableStateOf(0)
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("amt_settings", Context.MODE_PRIVATE)
        characterWpm = prefs.getFloat("charWpm", 18f).toDouble()
        effectiveWpm = prefs.getFloat("effWpm", 18f).toDouble()
        sidetoneHz = prefs.getFloat("sidetone", 600f).toDouble()
        hapticsEnabled = prefs.getBoolean("haptics", true)
        voiceAnswersEnabled = prefs.getBoolean("voiceAnswers", false)
        remindersEnabled = prefs.getBoolean("reminders", false)
        reminderHour = prefs.getInt("reminderHour", 19)
        reminderMinute = prefs.getInt("reminderMinute", 0)
    }

    /** The playback timing implied by the speed settings (Farnsworth when effective < character). */
    fun timing(): MorseTiming =
        if (effectiveWpm < characterWpm) MorseTiming.farnsworth(characterWpm, effectiveWpm)
        else MorseTiming(characterWpm)

    fun updateCharacterWpm(value: Double) {
        characterWpm = value.coerceIn(5.0, 40.0)
        if (effectiveWpm > characterWpm) effectiveWpm = characterWpm  // effective can't exceed character speed
        persist()
    }

    fun updateEffectiveWpm(value: Double) {
        effectiveWpm = value.coerceIn(5.0, characterWpm)
        persist()
    }

    fun updateSidetoneHz(value: Double) {
        sidetoneHz = value.coerceIn(300.0, 1000.0)
        persist()
    }

    fun updateHapticsEnabled(value: Boolean) {
        hapticsEnabled = value
        persist()
    }

    fun updateVoiceAnswersEnabled(value: Boolean) {
        voiceAnswersEnabled = value
        persist()
    }

    fun updateRemindersEnabled(value: Boolean) {
        remindersEnabled = value
        persist()
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        reminderHour = hour.coerceIn(0, 23)
        reminderMinute = minute.coerceIn(0, 59)
        persist()
    }

    private fun persist() {
        prefs.edit()
            .putFloat("charWpm", characterWpm.toFloat())
            .putFloat("effWpm", effectiveWpm.toFloat())
            .putFloat("sidetone", sidetoneHz.toFloat())
            .putBoolean("haptics", hapticsEnabled)
            .putBoolean("voiceAnswers", voiceAnswersEnabled)
            .putBoolean("reminders", remindersEnabled)
            .putInt("reminderHour", reminderHour)
            .putInt("reminderMinute", reminderMinute)
            .apply()
    }
}
