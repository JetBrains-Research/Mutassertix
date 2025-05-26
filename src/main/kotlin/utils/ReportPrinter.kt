package utils

import data.Report
import kotlin.collections.forEach

object ReportPrinter {
    /**
     * Displays a formatted output of mutation testing reports
     */
    fun print(report: MutableList<Report>) {
        // Define column headers and widths
        println("\nResults:")
        val format = "| %-30s | %15s | %15s | %15s |"
        val separator = "-".repeat(88)

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
}