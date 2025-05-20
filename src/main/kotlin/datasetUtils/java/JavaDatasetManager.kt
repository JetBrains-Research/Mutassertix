package datasetUtils.java

import data.ProjectConfiguration
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JavaDatasetManager {
    private val projectsDir = File("projects")

    /**
     * Parses a JSON file containing project configuration data and extracts the relevant configurations.
     *
     * @param filepath Path to the JSON file containing project configuration information
     * @return A list of ProjectConfiguration instances representing the extracted configurations
     */
    fun getProjectConfigurations(filepath: String): List<ProjectConfiguration> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonElement = json.parseToJsonElement(File(filepath).readText())

        val projectConfigurations = mutableListOf<ProjectConfiguration>()

        for (projectJson in jsonElement.jsonArray) {
            val github = projectJson.jsonObject["github"]?.jsonPrimitive?.content ?: ""
            val sourceDir = "${projectsDir.path}/${github.split("/").last()}"
            val buildTool = projectJson.jsonObject["buildTool"]?.jsonPrimitive?.content ?: ""

            if (github.isEmpty()) {
                printErrorMessage(github)
                continue
            }

            val projectDependencies = when (buildTool) {
                "maven" -> {
                    listOf(
                        "target/classes",
                        "target/test-classes"
                    )
                }

                "gradle" -> {
                    listOf(
                        "build/classes/java/main",
                        "build/classes/java/test"
                    )
                }

                else -> {
                    printErrorMessage(github)
                    continue
                }
            }

            val projectConfiguration = ProjectConfiguration(
                sourceDir = sourceDir,
                buildTool = buildTool,
                projectDependencies = projectDependencies,
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

            projectConfigurations.add(projectConfiguration)
        }

        return projectConfigurations
    }

    fun projectRebuild(projectConfiguration: ProjectConfiguration) {
//        TODO
    }

    private fun printErrorMessage(github: String) = println("> Error setting up project $github")
}
