package dataset

import data.ClassTestPair
import data.PropertiesReader
import data.ProjectConfig
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DatasetManager {
    private val projectsDir = File("projects")
    private val datasetJsonPath = "src/main/resources/java.json"

    fun setUpProjects(): List<ProjectConfig> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonElement = json.parseToJsonElement(File(datasetJsonPath).readText())

        val projectConfigs = mutableListOf<ProjectConfig>()

        // Iterate through each project in the JSON array
        for (projectJson in jsonElement.jsonArray) {
            val github = projectJson.jsonObject["github"]?.jsonPrimitive?.content ?: continue

            if (!cloneProject(github)) continue

            val projectName = github.split("/").last()
            val sourceDir = "${projectsDir.path}/$projectName"

            val buildTool = try {
                BuildTool.createByName(projectJson.jsonObject["buildTool"]?.jsonPrimitive?.content ?: "")
            } catch (_: IllegalArgumentException) {
                continue
            }

            // Extract the target class-test pairs
            val targetPairs = projectJson.jsonObject["targetPairs"]?.jsonArray?.map { pairJson ->
                val targetClass = pairJson.jsonObject["targetClass"]?.jsonPrimitive?.content ?: ""
                val targetTest = pairJson.jsonObject["targetTest"]?.jsonPrimitive?.content ?: ""
                ClassTestPair(targetClass, targetTest)
            } ?: emptyList()

            val projectConfig = ProjectConfig(
                projectName = sourceDir.split("/").last(),
                github = github,
                language = "Java",
                sourceDir = sourceDir,
                buildTool = buildTool,
                languagePath = PropertiesReader.javaPath,
                libraryDependencies = projectJson.jsonObject["libraryDependencies"]?.jsonArray?.map {
                    it.jsonPrimitive.content
                } ?: emptyList(),
                targetPairs = targetPairs
            )

            // Checks a project's build result
            if (!projectBuild(projectConfig).second) continue

            println("> Project \"$projectName\" has been added to the dataset")
            projectConfigs.add(projectConfig)
        }

        return projectConfigs
    }

    fun projectBuild(projectConfig: ProjectConfig): Pair<String, Boolean> {
        removeTargetFiles(projectConfig)

        val result = try {
            val shellCommand = """
                export JAVA_HOME=${projectConfig.languagePath} && ${projectConfig.buildTool.buildCommand}
            """.trimIndent()

            val process = ProcessBuilder("/bin/sh", "-c", shellCommand)
                .directory(File(projectConfig.sourceDir))
                .redirectErrorStream(true)
                .start()

            val outputMessage = process.inputStream.bufferedReader().use { it.readText() }

            process.waitFor()

            val successfulBuildComment = projectConfig.buildTool.successfulBuildComment

            if (outputMessage.contains(successfulBuildComment)) {
                successfulBuildComment
            } else {
                "Output:\n${
                    outputMessage.lines()
                        .filter { !it.trim().startsWith("[INFO]") }
                        .filter { !it.trim().startsWith("[WARNING]") }
                        .joinToString("\n")
                }"
            }
        } catch (e: Exception) {
            e.message ?: "Unknown error"
        }

        return Pair(result, result == projectConfig.buildTool.successfulBuildComment)
    }

    /**
     * Removes target directories associated with compiled class files from the project source directory.
     *
     * @param projectConfig The project configuration containing information about source directories,
     * dependencies, target classes, and tests.
     */
    private fun removeTargetFiles(projectConfig: ProjectConfig) {
        for (index in 0..1) File("${projectConfig.sourceDir}/${projectConfig.buildTool.projectDependencies[index]}").deleteRecursively()
    }

    /**
     * Clones a project repository from the given GitHub URL.
     *
     * @param github The URL of the GitHub repository to be cloned.
     * @return True if the repository was cloned successfully, otherwise false.
     */
    private fun cloneProject(github: String): Boolean {
        try {
            if (!projectsDir.exists()) {
                projectsDir.mkdir()
            }

            val projectName = github.split("/").last()
            val targetDir = "${projectsDir.path}/$projectName"

            if (File(targetDir).exists()) {
                File(targetDir).deleteRecursively()
            }

            val process = ProcessBuilder("git", "clone", "https://github.com/$github", targetDir).start()

            process.inputStream.bufferedReader().use { it.readText() }
            process.errorStream.bufferedReader().use { it.readText() }

            process.waitFor()

            return File(targetDir).exists()
        } catch (_: Exception) {
            return false
        }
    }
}
