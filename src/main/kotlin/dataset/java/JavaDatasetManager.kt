package dataset.java

import data.ClassTestPair
import data.PropertiesReader
import data.ProjectConfig
import dataset.DatasetManager
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import languages.LanguageConfig

class JavaDatasetManager : DatasetManager() {
    private val datasetJsonPath = "src/main/resources/java.json"

    override fun setUpProjects(languageConfig: LanguageConfig): List<ProjectConfig> {
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
                JavaBuildTool.createByName(projectJson.jsonObject["buildTool"]?.jsonPrimitive?.content ?: "")
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
                language = languageConfig.name,
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

    override fun projectBuild(projectConfig: ProjectConfig): Pair<String, Boolean> {
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

            val successfulBuildComment = (projectConfig.buildTool as JavaBuildTool).successfulBuildComment

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

        return Pair(result, result == (projectConfig.buildTool as JavaBuildTool).successfulBuildComment)
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
}
