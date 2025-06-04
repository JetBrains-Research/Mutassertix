package ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.local.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LLModel
import data.PropertiesReader
import data.ProjectConfiguration
import dataset.DatasetManager
import kotlinx.coroutines.runBlocking
import mutation.MutationPipeline
import org.jetbrains.research.mutassertix.agent.AgentUtils

object Agent {
    fun run(
        llmModel: LLModel,
        projectConfiguration: ProjectConfiguration,
        datasetManager: DatasetManager,
        mutationPipeline: MutationPipeline
    ) {
        val executor = AgentUtils.getExecutor(PropertiesReader.grazieToken)

        val toolRegistry = ToolRegistry {
            tools(Tools(projectConfiguration, datasetManager, mutationPipeline).asTools())
        }

        val strategy = strategy("basic-strategy") {
            val nodeCallLLM by nodeLLMRequest()
            val executeToolCall by nodeExecuteTool()
            val sendToolResult by nodeLLMSendToolResult()

            edge(nodeStart forwardTo nodeCallLLM)
            edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })
            edge(nodeCallLLM forwardTo executeToolCall onToolCall { true })
            edge(executeToolCall forwardTo sendToolResult)
            edge(sendToolResult forwardTo nodeFinish onAssistantMessage { true })
            edge(sendToolResult forwardTo executeToolCall onToolCall { true })
        }

        val agentConfig = AIAgentConfig(
            prompt = prompt("basic-strategy") {
                system(
                    """
                    You are an intelligent assertion generator.
                    Your goal is to increase the project's mutation score.

                    Do not finish agent execution before processing all test files.

                    Follow these steps per each test file separately:
                    1. Analysis
                       - Record current mutation score.
                       - Identify existing assertions.
                       - Identify target class under test.
                       - Look for uncovered scenarios in the target class under test file.

                    2. Enhancement
                       - Generate new assertions for the test file.
                       - Write new assertions to the test file.

                    3. Validation
                       - Build project. Fix errors if any. After 3 unsuccessful attempts reset the test file and proceed to next file.
                       - Calculate new mutation score.
                       - If score is 100: proceed to next file.
                       - After 3 attempts proceed to next file.
                """.trimIndent()
                )
            },
            model = llmModel,
            maxAgentIterations = 1000
        )

        val agent = AIAgent(
            promptExecutor = executor,
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = toolRegistry
        ) {
            handleEvents {
                onToolCall = { tool: Tool<*, *>, toolArgs: Tool.Args ->
                    println("> Agent: tool called - tool ${tool.name}, args - $toolArgs")
                }

                onAgentRunError = { _: String, throwable: Throwable ->
                    println("> Agent: ERROR - ${throwable.message}\n${throwable.stackTraceToString()}")
                }

                onAgentFinished = { _: String, result: String? ->
                    println("> Agent: result - $result")
                }
            }
        }

        runBlocking {
            agent.run(
                """
                Project Details:
                - Programming Language: ${projectConfiguration.language}
                - Build System: ${projectConfiguration.buildTool}
                - Source Location: ${projectConfiguration.sourceDir}
                
                Target Scope:
                - Classes under test: ${projectConfiguration.targetClasses.joinToString(", ")}
                - Test classes: ${projectConfiguration.targetTests.joinToString(", ")}
                """.trimIndent()
            )
        }
    }
}