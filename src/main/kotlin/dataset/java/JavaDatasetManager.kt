package dataset.java

import data.PropertiesReader
import data.ProjectConfiguration
import dataset.DatasetManager
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import languages.LanguageConfig

class JavaDatasetManager : DatasetManager() {
    private val datasetJsonPath = "src/main/resources/java.json"

    override fun setUpProjects(languageConfig: LanguageConfig): List<ProjectConfiguration> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonElement = json.parseToJsonElement(File(datasetJsonPath).readText())

        val projectConfigurations = mutableListOf<ProjectConfiguration>()

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

            val projectConfiguration = ProjectConfiguration(
                projectName = sourceDir.split("/").last(),
                github = github,
                language = languageConfig.name,
                sourceDir = sourceDir,
                buildTool = buildTool,
                languagePath = PropertiesReader.javaPath,
                libraryDependencies = projectJson.jsonObject["libraryDependencies"]?.jsonArray?.map {
                    it.jsonPrimitive.content
                } ?: emptyList(),
                targetClasses = projectJson.jsonObject["targetClasses"]?.jsonArray?.map {
                    it.jsonPrimitive.content
                } ?: emptyList(),
                targetTests = projectJson.jsonObject["targetTests"]?.jsonArray?.map {
                    it.jsonPrimitive.content
                } ?: emptyList()
            )

            if (projectBuild(projectConfiguration) != (buildTool as JavaBuildTool).successfulBuildComment) continue

            println("> Project \"$projectName\" has been added to the dataset")
            projectConfigurations.add(projectConfiguration)
        }

        return projectConfigurations
    }

    override fun projectBuild(projectConfiguration: ProjectConfiguration): String {
        removeTargetFiles(projectConfiguration)

        val result = try {
            val shellCommand = """
                export JAVA_HOME=${projectConfiguration.languagePath} && ${projectConfiguration.buildTool.buildCommand}
            """.trimIndent()

            val process = ProcessBuilder("/bin/sh", "-c", shellCommand)
                .directory(File(projectConfiguration.sourceDir))
                .redirectErrorStream(true)
                .start()

            val outputMessage = process.inputStream.bufferedReader().use { it.readText() }

            process.waitFor()

            val successfulBuildComment = (projectConfiguration.buildTool as JavaBuildTool).successfulBuildComment

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

        println("> Build result: $result")

        return result
    }

    /**
     * Removes target directories associated with compiled class files from the project source directory.
     *
     * @param projectConfiguration The project configuration containing information about source directories,
     * dependencies, target classes, and tests.
     */
    private fun removeTargetFiles(projectConfiguration: ProjectConfiguration) {
        for (index in 0..1) File("${projectConfiguration.sourceDir}/${projectConfiguration.buildTool.projectDependencies[index]}").deleteRecursively()
    }
}
