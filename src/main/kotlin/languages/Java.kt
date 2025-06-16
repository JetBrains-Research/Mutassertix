package languages

import languages.LanguageConfig
import dataset.java.JavaDatasetManager
import mutation.java.JavaMutationPipeline

class Java : LanguageConfig(
    name = "Java",
    datasetManager = JavaDatasetManager(),
    mutationPipeline = JavaMutationPipeline(),
)
