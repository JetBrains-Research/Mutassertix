import data.ProjectConfiguration
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A utility class for parsing JSON files and extracting project configurations.
 */
class JsonParser {

    /**
     * Parses a JSON file containing project configuration data and extracts the relevant configurations.
     *
     * @param filepath Path to the JSON file containing project configuration information
     * @return A list of ProjectConfiguration instances representing the extracted configurations
     */
    fun getProjectConfigurations(filepath: String): List<ProjectConfiguration> {
        val json = Json { ignoreUnknownKeys = true }
        val jsonElement = json.parseToJsonElement(File(filepath).readText())

        return jsonElement.jsonObject["projects"]?.jsonArray?.flatMap { projectJson ->
            val sourceDir = projectJson.jsonObject["sourceDir"]?.jsonPrimitive?.content ?: ""
            val buildTool = projectJson.jsonObject["buildTool"]?.jsonPrimitive?.content ?: ""
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
                    emptyList()
                }
            }

            projectJson.jsonObject["targets"]?.jsonArray?.map { target ->
                ProjectConfiguration(
                    sourceDir = sourceDir,
                    projectDependencies = projectDependencies,
                    libraryDependencies = projectJson.jsonObject["libraryDependencies"]?.jsonArray?.map {
                        it.jsonPrimitive.content
                    } ?: emptyList(),
                    targetClass = target.jsonObject["class"]?.jsonPrimitive?.content ?: "",
                    targetTest = target.jsonObject["test"]?.jsonPrimitive?.content ?: ""
                )
            } ?: emptyList()
        } ?: emptyList()
    }
}
