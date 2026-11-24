package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "VoiceController"

class VoiceController(context: Context) {
    private val appContext = context.applicationContext

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsInitialized = true
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            _speakingMessageId.value = null
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _speakingMessageId.value = null
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _speakingMessageId.value = null
                        }
                    })
                } else {
                    Log.w(TAG, "TextToSpeech init failed with status=$status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    fun clearError() {
        _voiceError.value = null
    }

    fun startListening(
        hinglishPreferred: Boolean,
        onFinalResult: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _voiceError.value = "Speech recognition service is not available on this device."
            return
        }

        stopSpeaking()
        stopListening()

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _partialTranscript.value = ""
                    _voiceError.value = null
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _partialTranscript.value = ""
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try speaking clearly."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Network required for speech recognition."
                        else -> "Voice input stopped (code $error)."
                    }
                    if (error != SpeechRecognizer.ERROR_CLIENT) {
                        _voiceError.value = msg
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim().orEmpty()
                    _partialTranscript.value = ""
                    if (spoken.isNotEmpty()) {
                        onFinalResult(spoken)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()?.trim().orEmpty()
                    if (partial.isNotEmpty()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    if (hinglishPreferred) "en-IN" else Locale.getDefault().toLanguageTag()
                )
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _isListening.value = false
            _voiceError.value = "Unable to start microphone: ${e.localizedMessage ?: "error"}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _partialTranscript.value = ""
        }
    }

    fun toggleSpeak(messageId: String, rawMarkdownText: String, hinglishPreferred: Boolean) {
        if (_speakingMessageId.value == messageId) {
            stopSpeaking()
            return
        }
        val tts = textToSpeech
        if (!ttsInitialized || tts == null) {
            _voiceError.value = "Text-to-speech engine is not ready yet."
            return
        }

        val cleanText = sanitizeForSpeech(rawMarkdownText)
        if (cleanText.isBlank()) return

        val hasDevanagari = cleanText.any { it in '\u0900'..'\u097F' }
        val targetLocale = when {
            hasDevanagari -> Locale("hi", "IN")
            hinglishPreferred -> Locale("en", "IN")
            else -> Locale.US
        }
        val availability = tts.isLanguageAvailable(targetLocale)
        if (availability >= TextToSpeech.LANG_AVAILABLE) {
            tts.language = targetLocale
        } else {
            tts.language = Locale.US
        }

        _speakingMessageId.value = messageId
        val utteranceId = UUID.randomUUID().toString()
        tts.speak(cleanText.take(3900), TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {
        }
        _speakingMessageId.value = null
    }

    private fun sanitizeForSpeech(markdown: String): String {
        return markdown
            .replace(Regex("\\[CHART\\][\\s\\S]*?\\[/CHART\\]"), " See the chart below. ")
            .replace(Regex("\\[FOLLOWUPS\\][\\s\\S]*?\\[/FOLLOWUPS\\]"), "")
            .replace(Regex("```[\\s\\S]*?```"), " Code block omitted. ")
            .replace(Regex("`([^`]*)`"), "$1")
            .replace(Regex("[#*_>|~]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun release() {
        stopListening()
        stopSpeaking()
        try {
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }
        textToSpeech = null
        ttsInitialized = false
    }
}
