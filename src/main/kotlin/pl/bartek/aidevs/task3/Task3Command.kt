package pl.bartek.aidevs.task3

import org.springframework.beans.factory.annotation.Value
import org.springframework.shell.command.CommandContext
import org.springframework.shell.command.annotation.Command
import org.springframework.web.client.RestClient
import pl.bartek.aidevs.AiModelVendor
import pl.bartek.aidevs.courseapi.AiDevsAnswer
import pl.bartek.aidevs.courseapi.AiDevsApiClient
import pl.bartek.aidevs.courseapi.Task

@Command(group = "task")
class Task3Command(
    @Value("\${aidevs.api-key}") private val apiKey: String,
    @Value("\${aidevs.task.3.data-url}") private val dataUrl: String,
    @Value("\${aidevs.task.3.answer-url}") private val answerUrl: String,
    aiModelVendor: AiModelVendor,
    private val aiDevsApiClient: AiDevsApiClient,
    private val restClient: RestClient,
) {
    private val chatClient = aiModelVendor.defaultChatClient()

    @Command(command = ["task3"])
    fun run(ctx: CommandContext) {
        val industrialRobotCalibrationFile = fetchInputData()
        val newTestData =
            industrialRobotCalibrationFile.testData.map { testDataItem ->
                val answer =
                    testDataItem.question
                        .split("\\+".toRegex())
                        .map { it.trim() }
                        .map { it.toInt() }
                        .reduce(Int::plus)

                val testQuestion =
                    testDataItem.test?.let {
                        ctx.terminal.writer().println(it.question)
                        ctx.terminal.writer().flush()
                        val response =
                            chatClient
                                .prompt(it.question)
                                .call()
                                .content() ?: throw IllegalStateException("Cannot get answer")
                        ctx.terminal.writer().println(response)
                        ctx.terminal.writer().flush()

                        TestQuestion(it.question, response)
                    }

                TestData(testDataItem.question, answer.toString(), testQuestion)
            }

        val answer =
            aiDevsApiClient.sendAnswer(
                answerUrl,
                AiDevsAnswer(
                    Task.JSON,
                    industrialRobotCalibrationFile.copy(apiKey = apiKey, testData = newTestData),
                ),
            )
        ctx.terminal.writer().println(answer)
        ctx.terminal.writer().flush()
    }

    private fun fetchInputData(): IndustrialRobotCalibrationFile =
        restClient
            .get()
            .uri(dataUrl, apiKey)
            .retrieve()
            .body(IndustrialRobotCalibrationFile::class.java) ?: throw IllegalStateException("Cannot get data to process")
}
