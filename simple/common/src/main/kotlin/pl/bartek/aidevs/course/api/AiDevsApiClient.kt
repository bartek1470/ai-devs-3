package pl.bartek.aidevs.course.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URL

@Component
class AiDevsApiClient(
    @Value("\${aidevs.api-key}") private val apiKey: String,
    private val objectMapper: ObjectMapper,
    private val restClient: RestClient,
) {
    fun <T> sendAnswer(
        url: URL,
        answer: AiDevsAnswer<T>,
    ): AiDevsAnswerResponse {
        val authenticatedAnswer = AiDevsAuthenticatedAnswer(answer.task.taskName, answer.answer, apiKey)
        val responseSpec =
            restClient
                .post()
                .uri(url.toURI())
                .headers { headers ->
                    headers.add(ACCEPT, APPLICATION_JSON_VALUE)
                    headers.add(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                }.body(objectMapper.writeValueAsString(authenticatedAnswer))
                .retrieve()

        return responseSpec.body()
            ?: throw IllegalStateException("Missing response body")
    }

    fun <T> sendAnswerReceiveText(
        url: URL,
        answer: AiDevsAnswer<T>,
    ): String {
        val authenticatedAnswer = AiDevsAuthenticatedAnswer(answer.task.taskName, answer.answer, apiKey)
        val responseSpec =
            restClient
                .post()
                .uri(url.toURI())
                .headers { headers ->
                    headers.add(ACCEPT, APPLICATION_JSON_VALUE)
                    headers.add(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                }.body(objectMapper.writeValueAsString(authenticatedAnswer))
                .retrieve()

        return responseSpec.body()
            ?: throw IllegalStateException("Missing response body")
    }
}
