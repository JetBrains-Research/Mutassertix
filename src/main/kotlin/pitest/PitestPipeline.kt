package pitest

class PitestPipeline {

    /**
     * Runs the Pitest pipeline
     *
     * @param sourceDir Root directory containing source files
     * @param projectDependencies List of project-specific dependencies to be included in classpath
     * @param targetClasses Pattern specifying which classes to mutate (e.g. "com.example.*")
     * @param targetTests Pattern specifying which tests to run (e.g. "com.example.*Test")
     * @return Complete command line string for executing PITest
     */
    fun getMutationScore(
        sourceDir: String,
        projectDependencies: List<String>,
        targetClasses: String,
        targetTests: String
    ): Int {
        val pitestCommandLine =
            PitestCommandLineGenerator().getCommand(sourceDir, projectDependencies, targetClasses, targetTests)

        val process = ProcessBuilder()
            .command("sh", "-c", pitestCommandLine)
            .start()

        // Read output
        val output = process.inputStream.bufferedReader().use { it.readText() }

        process.waitFor()

        return PitestReportParser().getMutationScore(output)
    }
}
