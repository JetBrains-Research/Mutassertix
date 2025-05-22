package datasetUtils.java

import data.ProjectConfiguration
import datasetUtils.DatasetManager
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JavaDatasetManager: DatasetManager {
    private val projectsDir = File("projects")

    override fun setUpProjects(filepath: String): List<ProjectConfiguration> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonElement = json.parseToJsonElement(File(filepath).readText())

        val projectConfigurations = mutableListOf<ProjectConfiguration>()

        // Iterate through each project in the JSON array
        for (projectJson in jsonElement.jsonArray) {
            val github = projectJson.jsonObject["github"]?.jsonPrimitive?.content ?: continue
            val projectName = github.split("/").last()
            val sourceDir = "${projectsDir.path}/$projectName"

            val buildTool = try {
                JavaBuildTool.createByName(projectJson.jsonObject["buildTool"]?.jsonPrimitive?.content ?: "")
            } catch (_: IllegalArgumentException) {
                continue
            }

            val projectDependencies = buildTool.projectDependencies

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

            println("> Project \"$projectName\" has been added to the dataset")
            projectConfigurations.add(projectConfiguration)
        }

        return projectConfigurations
    }

    override fun projectRebuild(projectConfiguration: ProjectConfiguration) {
//        TODO
    }
}
