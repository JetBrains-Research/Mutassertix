package utils

import java.io.File

object FilesUtils {
    /**
     * Caches the data of a project based on the specified parameters.
     * Copies the content of the project's source directory to a target directory within a cache directory.
     *
     * @param projectName The name of the project whose data is to be cached.
     */
    fun cacheProject(projectName: String) {
        val sourceDir = File("projects/$projectName")
        val cacheDir = File("cache")
        val targetDir = File(cacheDir, projectName)

        if (!cacheDir.exists()) {
            cacheDir.mkdir()
        }

        sourceDir.copyRecursively(targetDir, overwrite = true)
    }

    /**
     * Searches for the file path of a Java class within a given source directory.
     *
     * @param sourceDirPath The root directory path where the search operation starts.
     * @param className The fully qualified name of the Java class (e.g., com.example.MyClass).
     * @return A pair containing the absolute file path of the class file (if found) and a boolean indicating
     *         whether the search was successful. If unsuccessful, the first element contains an error message.
     */
    fun findClassFilePath(
        sourceDirPath: String,
        className: String
    ): Pair<String, Boolean> {
        val sourceDir = File(sourceDirPath)

        if (!sourceDir.exists()) return Pair("The file path '$sourceDirPath' does not exist.", false)

        val classPath = className.replace(".", File.separator) + ".java"

        return searchFile(sourceDir, classPath)
    }

    private fun searchFile(dir: File, classPath: String): Pair<String, Boolean> {
        val noSuchFileMessage = "No such file"

        if (!dir.isDirectory) return Pair(noSuchFileMessage, false)

        val files = dir.listFiles() ?: return Pair(noSuchFileMessage, false)

        for (file in files) {
            if (file.isDirectory) {
                val result = searchFile(file, classPath)
                if (result.second) {
                    // Only return if the file was found in the subdirectory
                    return result
                }
                // Otherwise continue searching for other files/directories
            } else if (file.path.endsWith(classPath)) {
                return Pair(file.absolutePath, true)
            }
        }
        return Pair(noSuchFileMessage, false)
    }

    /**
     * Retrieves the hierarchical file structure of a project starting from the specified path.
     *
     * @param path the root directory or file path from which to generate the file structure.
     * @param maxDepth the maximum depth in the directory hierarchy to traverse.
     * @return a string representation of the directory and file structure, or an error message if the path does not exist.
     */
    fun getProjectFileStructure(
        path: String,
        maxDepth: Int,
    ): String {
        val currentFile = File(path)

        if (!currentFile.exists()) return "The file path '$path' does not exist."

        return buildStructure(currentFile, path, 0, maxDepth)
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
