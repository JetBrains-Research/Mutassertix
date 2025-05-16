import data.Report
import pitest.PitestPipeline

/**
 * Main pipeline implementation
 */
fun main() {
    val projectConfigurations = JsonParser().getProjectConfigurations("src/main/resources/dataset.json")

    val reports = mutableListOf<Report>()

    for (projectConfiguration in projectConfigurations) {
        val projectName = projectConfiguration.sourceDir.split("/").last()

        println("> Running the pipeline for project \"$projectName\"")

        println("> Running first mutation process")
        val initialMutationScore = PitestPipeline().getMutationScore(projectConfiguration)

        println("> Running AI assertion generation process")
        // TODO AI implementation
        // TODO rebuilding project

        println("> Running second mutation process")
        val finalMutationScore = PitestPipeline().getMutationScore(projectConfiguration)

        reports.add(
            Report(
                projectName,
                initialMutationScore,
                finalMutationScore,
                finalMutationScore - initialMutationScore
            )
        )
    }
    reportsOutput(reports)
}

/**
 * Displays a formatted output of mutation testing reports
 */
fun reportsOutput(report: MutableList<Report>) {
    // Define column headers and widths
    println("\nResults:")
    val format = "| %-25s | %15s | %15s | %15s |"
    val separator = "-".repeat(83)

    // Print header
    println(separator)
    println(
        String.format(
            format,
            "Project",
            "Initial Score",
            "Final Score",
            "Difference"
        )
    )
    println(separator)

    // Print each report row
    report.forEach { r ->
        println(
            String.format(
                format,
                r.projectName,
                r.initialMutationScore.toString(),
                r.finalMutationScore.toString(),
                r.difference.toString()
            )
        )
    }
    println(separator)
}
