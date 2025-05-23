package ai

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import java.io.File

@LLMDescription("Tools for operations with project files")
class Tools : ToolSet {
    @Tool
    @LLMDescription("Returns a text from the file")
    fun readTextFromFile(
        @LLMDescription("Path to the file")
        path: String,
    ): String {
        return try {
            File(path).readText()
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Modifies the text in the file")
    fun writeTextToFile(
        @LLMDescription("Path to the file")
        path: String,
        @LLMDescription("New file content")
        text: String,
    ) {
        File(path).writeText(text)
    }
}
