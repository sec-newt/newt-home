package io.github.secnewt.home.apps

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.widget.Toast

/**
 * Speaks a sentence with the phone's text-to-speech voice (for example RHVoice or eSpeak NG
 * from F-Droid), like Newt Phone's caller announcement. Started on first use and released when
 * the home screen goes away. With no voice installed, it shows the words instead.
 */
class Speaker(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var waiting: String? = null

    fun say(text: String) {
        val engine = tts
        if (engine != null && ready) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE)
            return
        }
        waiting = text
        if (engine != null) return // still starting; it speaks when ready
        tts = TextToSpeech(context.applicationContext) { status ->
            val started = tts
            if (status != TextToSpeech.SUCCESS || started == null) {
                Toast.makeText(context, waiting ?: text, Toast.LENGTH_LONG).show()
                release()
                return@TextToSpeech
            }
            started.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            ready = true
            waiting?.let { started.speak(it, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE) }
            waiting = null
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        waiting = null
    }

    private companion object {
        const val UTTERANCE = "newt-home"
    }
}
