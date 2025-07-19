import data.ProjectConfiguration
import java.io.File
import languages.LanguageConfig

object Pipeline {
    fun run(languageConfig: LanguageConfig, projectConfiguration: ProjectConfiguration, testIndex: Int) {
        val mutationResult = languageConfig.mutationPipeline.run(projectConfiguration, testIndex)

        if (mutationResult.mutationScore == 100) return
    }
}
