package app.anothermorsetrainer

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper

/**
 * Listens for key events from a MIDI keyer (the Vail Adapter or a BLE-MIDI key)
 * and reports each as a simple key-down/up, for hardware sending practice.
 *
 * The Vail Adapter sends MIDI note-on/off on channel 1: note 0 = straight key,
 * 1/20/61 = dit paddle, 2/21/62 = dah paddle; velocity > 0 = pressed. The adapter
 * does any iambic timing itself, so — exactly like the iOS `MIDIInput` →
 * `SendingKeyer` path — we collapse every key to a single down/up burst and let
 * [MorseDecoder] time it.
 *
 * Port of MorseTrainerApp/MIDIInput.swift (CoreMIDI → `android.media.midi`).
 * Callbacks are marshaled to the main thread so they can drive Compose state.
 */
class MidiKeyInput(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private var manager: MidiManager? = null
    private val openDevices = mutableListOf<MidiDevice>()
    private val openPorts = mutableListOf<MidiOutputPort>()
    private var onKey: ((Boolean) -> Unit)? = null
    private var onConnected: ((String?) -> Unit)? = null

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) = connect(device)
    }

    /** True on devices that expose any MIDI support at all. */
    val isSupported: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)

    /**
     * Begin listening. [onKey] fires (on the main thread) with `true` on key-down
     * and `false` on key-up; [onConnected] reports the connected device's name
     * (or null when none is attached yet).
     */
    // getDevices + the Handler-based registerDeviceCallback are deprecated in API
    // 33 in favor of transport/executor variants that need API 33 — the older
    // forms are the correct cross-version choice at minSdk 24.
    @Suppress("DEPRECATION")
    fun start(onKey: (Boolean) -> Unit, onConnected: (String?) -> Unit) {
        this.onKey = onKey
        this.onConnected = onConnected
        if (!isSupported) { onConnected(null); return }
        val mgr = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager ?: run {
            onConnected(null); return
        }
        manager = mgr
        mgr.devices.forEach { connect(it) }
        mgr.registerDeviceCallback(deviceCallback, main)
        if (openPorts.isEmpty()) onConnected(null)
    }

    fun stop() {
        manager?.let { try { it.unregisterDeviceCallback(deviceCallback) } catch (_: Exception) {} }
        openPorts.forEach { try { it.close() } catch (_: Exception) {} }
        openDevices.forEach { try { it.close() } catch (_: Exception) {} }
        openPorts.clear(); openDevices.clear()
        manager = null
        onKey = null; onConnected = null
    }

    private fun connect(info: MidiDeviceInfo) {
        if (info.outputPortCount <= 0) return   // we read FROM the key's output port
        val mgr = manager ?: return
        mgr.openDevice(info, { device ->
            if (device == null) return@openDevice
            openDevices.add(device)
            val port = device.openOutputPort(0) ?: return@openDevice
            port.connect(KeyReceiver())
            openPorts.add(port)
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            main.post { onConnected?.invoke(name) }
        }, main)
    }

    /** Parses raw MIDI bytes into Vail-style key down/up events. */
    private inner class KeyReceiver : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            if (count < 3) return
            val status = msg[offset].toInt() and 0xF0
            val note = msg[offset + 1].toInt() and 0x7F
            val velocity = msg[offset + 2].toInt() and 0x7F

            val isDown = when {
                status == 0x90 && velocity > 0 -> true
                status == 0x80 -> false
                status == 0x90 -> false   // Note On, velocity 0 = Note Off
                else -> return
            }
            // Map only the adapter's keyer notes; ignore anything else so an
            // unrelated MIDI synth never registers as keying.
            when (note) {
                0, 1, 20, 61, 2, 21, 62 -> main.post { onKey?.invoke(isDown) }
                else -> return
            }
        }
    }
}
