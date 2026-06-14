package app.anothermorsetrainer.vail

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.anothermorsetrainer.MidiKeyInput
import app.anothermorsetrainer.SidetoneGenerator
import kotlin.math.pow
import kotlin.random.Random

/**
 * Top-level orchestrator for the Vail repeater: owns the [VailClient], the local
 * TX [SidetoneGenerator], the RX [RepeaterTonePlayer], and the optional MIDI key.
 * Wires key down/up → local sidetone + (when break-in is on) a network
 * transmission, and inbound tones → clock-synced playback. Holds Compose state.
 *
 * Port of MorseTrainerApp/RepeaterModel.swift, trimmed to the core repeater
 * (the adapter-piezo/MIDIOutput keyer-config path is hardware-specific and left
 * out). Keeps clock sync, break-in, the 10 s stuck-key cutoff, and reconnect.
 */
class VailRepeater(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("amt_vail", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())

    private val client = VailClient(callsign = "", txTone = 72)
    private val tonePlayer = RepeaterTonePlayer()
    private val midi = MidiKeyInput(context)
    private var sidetone: SidetoneGenerator? = null

    // ---- Compose state ----
    var connectionState by mutableStateOf(ConnectionState.DISCONNECTED); private set
    var callsign by mutableStateOf(""); private set
    var channel by mutableStateOf("General"); private set
    var txTone by mutableStateOf(72); private set
    var rxDelayMs by mutableStateOf(2000); private set
    var breakInEnabled by mutableStateOf(false); private set
    var users by mutableStateOf<List<VailMessage.UserInfo>>(emptyList()); private set
    var notice by mutableStateOf<String?>(null); private set
    var lagMs by mutableStateOf(0L); private set
    var isKeying by mutableStateOf(false); private set
    var midiDevice by mutableStateOf<String?>(null); private set

    private var keyDown = false
    private var keyBeginMs = 0L
    private val stuckKey = Runnable { handleStuckKey() }

    init {
        callsign = prefs.getString("callsign", null)?.takeIf { it.isNotBlank() } ?: anonCallsign()
        channel = prefs.getString("channel", null)?.takeIf { it.isNotBlank() } ?: "General"
        txTone = prefs.getInt("txTone", 72)
        rxDelayMs = prefs.getInt("rxDelay", 2000)
        breakInEnabled = prefs.getBoolean("breakIn", false)
        client.callsign = callsign
        client.txTone = txTone
    }

    fun start() {
        client.onEvent = { handleEvent(it) }
        sidetone = SidetoneGenerator(midiToHz(txTone)).also { it.start() }
        midi.start(
            onKey = { down -> touchKey(down) },
            onConnected = { midiDevice = it }
        )
    }

    fun stop() {
        main.removeCallbacks(stuckKey)
        client.disconnect()
        client.onEvent = null
        midi.stop()
        sidetone?.stop(); sidetone = null
        tonePlayer.release()
    }

    // ---- Connection ----

    fun connect() {
        val isDecoder = channel.equals("Decoder", ignoreCase = true)
        client.connect(channel, isPrivate = !isDecoder, isDecoder = isDecoder)
    }

    fun disconnect() = client.disconnect()

    // ---- Config ----

    fun updateCallsign(value: String) {
        val t = value.trim()
        if (t.isEmpty()) return
        callsign = t; prefs.edit().putString("callsign", t).apply()
        client.updateCallsign(t)
    }

    fun updateChannel(value: String) {
        val t = value.trim().ifEmpty { "General" }
        channel = t; prefs.edit().putString("channel", t).apply()
    }

    fun updateTxTone(note: Int) {
        val n = note.coerceIn(48, 96)
        txTone = n; prefs.edit().putInt("txTone", n).apply()
        client.updateTxTone(n)
        // Sidetone pitch follows the TX tone.
        sidetone?.let { it.stop(); sidetone = SidetoneGenerator(midiToHz(n)).also { s -> s.start() } }
    }

    fun updateRxDelayMs(ms: Int) {
        rxDelayMs = ms.coerceIn(0, 5000); prefs.edit().putInt("rxDelay", rxDelayMs).apply()
    }

    fun setBreakIn(enabled: Boolean) {
        breakInEnabled = enabled; prefs.edit().putBoolean("breakIn", enabled).apply()
    }

    fun sendChat(text: String) = client.sendChat(text)

    // ---- Keying ----

    fun touchKey(isDown: Boolean) {
        val nowMs = System.currentTimeMillis()
        if (isDown && !keyDown) {
            keyDown = true
            keyBeginMs = nowMs
            isKeying = true
            sidetone?.setKeyDown(true)
            startStuckKeyWatchdog()
        } else if (!isDown && keyDown) {
            keyDown = false
            isKeying = false
            sidetone?.setKeyDown(false)
            val duration = (nowMs - keyBeginMs).coerceAtLeast(0)
            if (breakInEnabled && duration in 1..65535) {
                client.transmitTone(duration.toInt(), keyBeginMs)
            }
            main.removeCallbacks(stuckKey)
        }
    }

    private fun startStuckKeyWatchdog() {
        main.removeCallbacks(stuckKey)
        main.postDelayed(stuckKey, 10_000)
    }

    private fun handleStuckKey() {
        keyDown = false
        isKeying = false
        sidetone?.setKeyDown(false)
        setBreakIn(false)
        notice = "Stuck key detected. Break-in disabled."
    }

    // ---- Client events ----

    private fun handleEvent(event: VailEvent) {
        when (event) {
            is VailEvent.StateChanged -> {
                connectionState = event.state
                if (event.state == ConnectionState.CONNECTING) users = emptyList()
                if (event.state == ConnectionState.CONNECTED) notice = null
            }
            is VailEvent.Tone -> {
                val note = event.txTone ?: 69
                tonePlayer.scheduleTone(note, event.durationMs, event.atLocalMs + rxDelayMs)
            }
            is VailEvent.Roster -> users = event.users
            is VailEvent.OwnEcho -> lagMs = event.lagMs
            is VailEvent.Notice -> notice = event.text
            is VailEvent.Chat -> { /* chat UI is a follow-up; notices cover status */ }
            is VailEvent.DecoderRoomChanged -> { /* informational */ }
        }
    }

    // ---- Helpers ----

    private fun midiToHz(note: Int): Double = 440.0 * 2.0.pow((note - 69) / 12.0)

    private fun anonCallsign(): String = "anon" + Random.nextInt(1000, 10000)
}
