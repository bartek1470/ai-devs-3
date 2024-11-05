package pl.bartek.aidevs.task2

data class PatrollingRobotConversation(
    var messageId: String = "0",
    val messages: MutableList<String> = mutableListOf(),
) {
    fun hasStarted(): Boolean = messageId != "0"
}
