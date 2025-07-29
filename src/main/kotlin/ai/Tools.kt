package ai

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import java.io.File
import utils.FilesUtils

@LLMDescription("A toolkit for file operations in a project")
class Tools : ToolSet {
    @Tool
    @LLMDescription("Reads and returns the content of a file at the specified path")
    fun readTextFromFile(
        @LLMDescription("Absolute or relative path to the file to read")
        path: String,
    ): String {
        return try {
            File(path).readText()
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Writes text content to a file at the specified path")
    fun writeTextToFile(
        @LLMDescription("Absolute or relative path to the file to write")
        path: String,
        @LLMDescription("Content to write to the file")
        text: String,
    ) {
        File(path).writeText(text)
    }

    @Tool
    @LLMDescription("Locates the file path of a Java class in the source directory")
    fun findClassFilePath(
        @LLMDescription("Root directory path to search for the class file")
        sourceDirPath: String,
        @LLMDescription("Fully qualified name of the Java class (e.g., com.example.MyClass)")
        className: String
    ): String = FilesUtils.findClassFilePath(sourceDirPath, className).first

    @Tool
    @LLMDescription("Retrieves a directory tree structure up to the specified depth")
    fun getProjectFileStructure(
        @LLMDescription("Starting directory path to generate structure from")
        path: String,
        @LLMDescription("Maximum directory depth to include in the structure")
        maxDepth: Int,
    ): String = FilesUtils.getProjectFileStructure(path, maxDepth)
}
