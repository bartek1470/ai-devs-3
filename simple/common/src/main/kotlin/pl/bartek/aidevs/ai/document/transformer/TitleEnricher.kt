package pl.bartek.aidevs.ai.document.transformer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.document.Document
import org.springframework.ai.document.DocumentTransformer
import org.springframework.stereotype.Component
import pl.bartek.aidevs.ai.ChatService
import pl.bartek.aidevs.ai.document.transformer.TitleEnricher.Companion.METADATA_TITLE
import pl.bartek.aidevs.config.AiDevsProperties

@Suppress("ktlint:standard:max-line-length")
private const val SYSTEM_MESSAGE = "Suggest a MOST SPECIFIC title for this text. You MUST answer only with the title text. You MUST NOT wrap text with quotation marks. The title MUST BE no longer than 7 words. You MUST NOT consider this message during title generation."

fun Document.title(): String = metadata[METADATA_TITLE]?.toString() ?: throw IllegalStateException("Invalid document. Missing title")

@Component
class TitleEnricher(
    private val aiDevsProperties: AiDevsProperties,
    private val chatService: ChatService,
) : DocumentTransformer {
    override fun apply(documents: List<Document>): List<Document> = documents.map { transformIfNeeded(it) }

    private fun transformIfNeeded(doc: Document): Document {
        if (doc.metadata[METADATA_TITLE] != null) {
            log.trace { "Document ${doc.id} already has a title" }
            return doc
        }

        log.info { "Processing document ${doc.id} text:\n${doc.text}" }
        val response =
            chatService.sendToChat(
                listOf(SystemMessage(SYSTEM_MESSAGE), UserMessage(doc.text)),
                chatOptions =
                    ChatOptions
                        .builder()
                        .model(aiDevsProperties.model.title)
                        .build(),
                streaming = false,
            )
        log.info { "Document ${doc.id} title: $response" }
        doc.metadata[METADATA_TITLE] = response
        return doc
    }

    companion object {
        private val log = KotlinLogging.logger { }
        const val METADATA_TITLE = "title"
    }
}
