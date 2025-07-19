package languages

import languages.LanguageConfig
import dataset.java.JavaDatasetManager
import mutation.JavaMutationPipeline

class Java : LanguageConfig(
    name = "Java",
    datasetManager = JavaDatasetManager(),
    mutationPipeline = JavaMutationPipeline(),
)
