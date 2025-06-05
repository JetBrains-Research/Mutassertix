package utils

import java.io.File

/**
 * Utility object for managing data caching operations related to projects and models.
 */
object CacheUtils {
    /**
     * Caches the data of a project based on the specified parameters.
     * Copies the content of the project's source directory to a target directory within a cache directory.
     *
     * @param projectName The name of the project whose data is to be cached.
     * @param index The index indicating repetition or a unique operation identifier.
     * @param llmModelId The identifier of the LLM model related to the caching operation.
     */
    fun cacheData(projectName: String, index: Int, llmModelId: String) {
        val sourceDir = File("projects/$projectName")
        val cacheDir = File("cache")
        val targetDir = File(cacheDir, "$projectName-$index-$llmModelId")

        if (!cacheDir.exists()) {
            cacheDir.mkdir()
        }

        sourceDir.copyRecursively(targetDir, overwrite = true)
    }
}