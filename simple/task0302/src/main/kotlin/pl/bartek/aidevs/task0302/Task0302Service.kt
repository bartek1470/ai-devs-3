package pl.bartek.aidevs.task0302

import io.github.oshai.kotlinlogging.KotlinLogging
import io.qdrant.client.QdrantClient
import org.jline.terminal.Terminal
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import pl.bartek.aidevs.config.AiDevsProperties
import pl.bartek.aidevs.config.Profile.QDRANT
import pl.bartek.aidevs.course.TaskId
import pl.bartek.aidevs.course.api.AiDevsAnswer
import pl.bartek.aidevs.course.api.AiDevsApiClient
import pl.bartek.aidevs.course.api.Task
import pl.bartek.aidevs.util.ReadFile
import pl.bartek.aidevs.util.println
import pl.bartek.aidevs.util.unzip
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

private const val DATE_METADATA_KEY = "report_date"

@Profile(QDRANT)
@Service
class Task0302Service(
    private val aiDevsProperties: AiDevsProperties,
    private val task0302Config: Task0302Config,
    @Value("\${spring.ai.vectorstore.qdrant.collection-name}") private val collectionName: String,
    private val aiDevsApiClient: AiDevsApiClient,
    private val restClient: RestClient,
    private val vectorStore: VectorStore,
    private val qdrantClient: QdrantClient,
) {
    private val cacheDir = aiDevsProperties.cacheDir.resolve(TaskId.TASK_0302.cacheFolderName())

    init {
        Files.createDirectories(this.cacheDir)
    }

    fun run(terminal: Terminal) {
        val documentCountInDb = qdrantClient.countAsync(collectionName, Duration.ofSeconds(5)).get()

        if (documentCountInDb == 0L) {
            val inputDataPath = fetchInputData()
            val documents =
                inputDataPath
                    .listDirectoryEntries("*.txt")
                    .map { ReadFile(it, Files.readString(it)) }
                    .map {
                        Document(it.content, mapOf(DATE_METADATA_KEY to it.name.replace("_", "-")))
                    }
            vectorStore.add(documents)
        }

        val result =
            vectorStore.similaritySearch(
                SearchRequest
                    .builder()
                    .query(
                        "W raporcie, z którego dnia znajduje się wzmianka o kradzieży prototypu broni?",
                    ).topK(1)
                    .build(),
            )

        terminal.println(result.toString())
        val date =
            result
                ?.get(0)
                ?.metadata
                ?.get(DATE_METADATA_KEY)
                ?.toString()

        val answer = aiDevsApiClient.sendAnswer(aiDevsProperties.reportUrl, AiDevsAnswer(Task.WEKTORY, date))
        terminal.println(answer)
    }

    private fun fetchInputData(): Path {
        val uriComponents =
            UriComponentsBuilder
                .fromUri(task0302Config.dataUrl.toURI())
                .build()
        val filename = uriComponents.pathSegments[uriComponents.pathSegments.size - 1]!!
        val zipFilePath = this.cacheDir.resolve(filename)
        val extractedZipPath = this.cacheDir.resolve(zipFilePath.nameWithoutExtension)

        val dataDir = "weapons_tests"
        val dataZipPath = this.cacheDir.resolve(zipFilePath.nameWithoutExtension).resolve("$dataDir.zip")
        val extractedDataZipPath = this.cacheDir.resolve(zipFilePath.nameWithoutExtension).resolve(dataDir)
        val inputDataPath = extractedDataZipPath.resolve("do-not-share")

        if (Files.exists(inputDataPath)) {
            log.info { "Input data already exists: ${inputDataPath.toAbsolutePath()}. Skipping" }
            return inputDataPath
        }

        val body =
            restClient
                .get()
                .uri(uriComponents.toUriString())
                .headers { it.contentType = MediaType.APPLICATION_OCTET_STREAM }
                .retrieve()
                .body(ByteArray::class.java)!!

        Files.newOutputStream(zipFilePath).use {
            it.write(body)
        }
        zipFilePath.unzip(extractedZipPath)
        Files.delete(zipFilePath)
        dataZipPath.unzip(
            extractedDataZipPath,
            task0302Config.dataPassword.toCharArray(),
        )
        Files.delete(dataZipPath)
        return inputDataPath
    }

    companion object {
        private val log = KotlinLogging.logger { }
    }
}
