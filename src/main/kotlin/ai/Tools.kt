package ai

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import data.ProjectConfiguration
import dataset.DatasetManager
import java.io.File
import mutation.MutationPipeline

@LLMDescription("A toolkit for file operations in a project")
class Tools(
    val projectConfiguration: ProjectConfiguration,
    val datasetManager: DatasetManager,
    val mutationPipeline: MutationPipeline
) : ToolSet {
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
    ): String {
        val sourceDir = File(sourceDirPath)

        if (!sourceDir.exists()) return "The file path '$sourceDirPath' does not exist."

        val classPath = className.replace(".", File.separator) + ".java"

        return searchFile(sourceDir, classPath) ?: "Class file not found for: $className"
    }

    @Tool
    @LLMDescription("Retrieves a directory tree structure up to the specified depth")
    fun getProjectFileStructure(
        @LLMDescription("Starting directory path to generate structure from")
        path: String,
        @LLMDescription("Maximum directory depth to include in the structure")
        maxDepth: Int,
    ): String {
        val currentFile = File(path)

        if (!currentFile.exists()) return "The file path '$path' does not exist."

        return buildStructure(currentFile, path, 0, maxDepth)
    }

    @Tool
    @LLMDescription("Builds the project")
    fun buildProject(): String = datasetManager.projectBuild(projectConfiguration)

    private fun searchFile(dir: File, classPath: String): String? {
        if (!dir.isDirectory) return null

        val files = dir.listFiles() ?: return null

        for (file in files) {
            if (file.isDirectory) {
                searchFile(file, classPath)?.let { return it }
            } else if (file.path.endsWith(classPath)) {
                return file.absolutePath
            }
        }
        return null
    }

    private fun buildStructure(file: File, path: String, currentDepth: Int, maxDepth: Int): String {
        val builder = StringBuilder()

        builder.append(path).append(file.name).append("\n")

        if (currentDepth >= maxDepth) return builder.toString()

        if (file.isDirectory) {
            val files = file.listFiles()
            if (!files.isNullOrEmpty()) {
                files.forEach { child ->
                    builder.append(buildStructure(child, "$path  ", currentDepth + 1, maxDepth))
                }
            }
        }

        return builder.toString()
    }
}
