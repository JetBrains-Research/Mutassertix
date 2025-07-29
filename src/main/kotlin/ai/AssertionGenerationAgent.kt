package ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.markdown.markdown
import data.ProjectConfig
import org.jetbrains.research.mutassertix.agent.PrivateAgentUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AssertionGenerationAgent(executor: PromptExecutor) {
    private val logger: Logger = LoggerFactory.getLogger(AssertionGenerationAgent::class.java)
    val toolRegistry = PublicAgentUtils.getToolRegistry()

    val strategy = PublicAgentUtils.createCommonStrategy("assertion-generation-agent-strategy")

    val agentConfig = AIAgentConfig(
        prompt = prompt("assertion-generation-agent-prompt") {
            system {
                markdown {
                    +"You are an intelligent assertion generator for language Java designed to improve project mutation scores."
                    +"You have to improve the assertion implementation of existing test files. "
                    +"DO NOT ADD ASSERTIONS FOR A NEW FUNCTIONALITY OF A CLASS UNDER TEST!"
                    h1("PROCESS OVERVIEW")
                    numbered {
                        item("Get the class under test file from the line, and get it's content")
                        item("Get the test class content from the user prompt")
                        item("Use the ThinkTool to analyze survived mutation in the class under test, and get a missed case in the test class")
                        item("Generate the assertion code for the missed case, which will kill the mutation")
                        item("Write the assertion code to the test file")
                    }
                }
            }
        },
        model = PrivateAgentUtils.getGPT4Model(),
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
        projectConfig: ProjectConfig,
        mutation: String,
        testIndex: Int
    ): Boolean {
        return try {
            agent.run(
                markdown {
                    h1("Project Details")
                    +"Programming Language: ${projectConfig.language}"
                    +"Build System: ${projectConfig.buildTool.name}"
                    +"Source Location: ${projectConfig.sourceDir}"

                    h1("Target Scope")
                    +"Test class: ${projectConfig.targetPairs[testIndex].targetTest}"

                    h1("Survived Mutation Data")
                    +mutation
                }
            )
            true
        } catch (e: Exception) {
            logger.error("Error in AssertionGenerationAgent: {}", e.message, e)
            false
        }
    }
}
