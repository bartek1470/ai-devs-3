package pl.bartek.aidevs.ai.transcript

/**
 * [Languages list](https://github.com/openai/whisper/blob/main/whisper/tokenizer.py)
 */
enum class WhisperLanguage(
    val code: String,
) {
    ENGLISH("en"),
    POLISH("pl"),
}
