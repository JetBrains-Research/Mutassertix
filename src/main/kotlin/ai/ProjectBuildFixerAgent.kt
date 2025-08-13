package ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.markdown.markdown
import data.ProjectConfig
import dataset.DatasetManager
import org.jetbrains.research.mutassertix.agent.PrivateAgentUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A specialized agent that aims to resolve compilation issues within a project's test files.
 *
 * @constructor Constructs a [ProjectBuildFixerAgent] agent with a prompt executor.
 *
 * @param executor The executor managing prompt execution to communicate with the LLM.
 */
class ProjectBuildFixerAgent(executor: PromptExecutor) {
    private val logger: Logger = LoggerFactory.getLogger(ProjectBuildFixerAgent::class.java)
    private val numberOfIterations = 3
    private val toolRegistry = PublicAgentUtils.getToolRegistry()

    val strategy = PublicAgentUtils.createCommonStrategy("project-build-fixer-agent-strategy")

    val agentConfig = AIAgentConfig(
        prompt = prompt("project-build-fixer-agent-prompt") {
            system {
                markdown {
                    +"You are an AI agent specialized in fixing compilation issues in the test file of a project."
                    +"Based on the provided file, project structure, and build output, you'll identify and resolve compilation errors by making necessary changes directly in the file."
                    +"You can build projects on your own."
                    +"IMPORTANT: you can modify ONLY the test class."
                }
            }
        },
        model = PrivateAgentUtils.getGPT4MiniModel(),
        maxAgentIterations = 500
    )

    val agent = AIAgent(
        promptExecutor = executor,
        strategy = strategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry
    ) {
        handleEvents {
            CustomEventHandler.create()()
        }
    }

    suspend fun run(
        datasetManager: DatasetManager,
        projectConfig: ProjectConfig,
        filePath: String,
    ): Boolean {
        repeat(numberOfIterations) {
            val projectBuildResult = datasetManager.projectBuild(projectConfig)
            if (projectBuildResult.second) return true
            val buildOutput = projectBuildResult.first
            try {
                agent.run(
                    markdown {
                        h1("Project Details")
                        +"Programming Language: ${projectConfig.language}"
                        +"Build System: ${projectConfig.buildTool.name}"
                        +"Source Location: ${projectConfig.sourceDir}"

                        h1("File Information")
                        +"File Path: $filePath"

                        h1("Build Output")
                        +buildOutput
                    }
                )
            } catch (e: Exception) {
                logger.error("Error in ProjectBuildFixer: {}", e.message, e)
                return false
            }
        }

        return datasetManager.projectBuild(projectConfig).second
    }
}
