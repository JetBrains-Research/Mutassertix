package datasetUtils.java

import data.Language
import data.ProjectConfiguration
import datasetUtils.DatasetManager
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JavaDatasetManager : DatasetManager() {
    override fun setUpProjects(filepath: String): List<ProjectConfiguration> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonElement = json.parseToJsonElement(File(filepath).readText())

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

            val projectDependencies = buildTool.projectDependencies

            val projectConfiguration = ProjectConfiguration(
                language = Language.JAVA,
                sourceDir = sourceDir,
                buildTool = buildTool,
                languagePath = projectJson.jsonObject["languagePath"]?.jsonPrimitive?.content ?: "",
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

            if (!projectBuild(projectConfiguration)) continue

            println("> Project \"$projectName\" has been added to the dataset")
            projectConfigurations.add(projectConfiguration)
        }

        return projectConfigurations
    }

    override fun projectBuild(projectConfiguration: ProjectConfiguration): Boolean {
        try {
            val shellCommand = """
                export JAVA_HOME=${projectConfiguration.languagePath} && ${projectConfiguration.buildTool.buildCommand}
            """.trimIndent()

            val process = ProcessBuilder("/bin/sh", "-c", shellCommand)
                .directory(File(projectConfiguration.sourceDir))
                .start()

            process.inputStream.bufferedReader().use { it.readText() }
            process.errorStream.bufferedReader().use { it.readText() }

            process.waitFor()

            return checkTargetFilesExist(projectConfiguration)
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Verifies the existence of the compiled class files for the specified target classes and tests
     * in the project configuration.
     *
     * @param projectConfiguration The project configuration containing information about source directories,
     * dependencies, target classes, and tests.
     * @return True if all the target class and test files exist, otherwise false.
     */
    private fun checkTargetFilesExist(projectConfiguration: ProjectConfiguration): Boolean {
        fun buildClassFilePath(className: String, dependencyIndex: Int): String =
            "${projectConfiguration.sourceDir}/${projectConfiguration.buildTool.projectDependencies[dependencyIndex]}/${
                className.replace(".", "/")
            }.class"

        fun checkFilesExist(targetFile: List<String>, dependencyIndex: Int): Boolean =
            targetFile.all { className ->
                File(buildClassFilePath(className, dependencyIndex)).exists()
            }

        return checkFilesExist(projectConfiguration.targetClasses, 0) &&
                checkFilesExist(projectConfiguration.targetTests, 1)
    }
}
