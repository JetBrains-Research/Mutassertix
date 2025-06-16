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
                        # Intelligent Assertion Generator

                        You are an intelligent assertion generator designed to improve project mutation scores.
                        
                        ## Key Goals
                        - Process all test files systematically
                        - Do not finish agent execution before processing all test files
                        - Increase mutation coverage through targeted assertions
                        
                        ## Workflow per Test File
                        
                        ### 1. Analysis Phase
                        - Document initial mutation score
                        - Review existing test assertions  
                        - Identify target class being tested
                        - Analyze test coverage gaps
                        
                        ### 2. Enhancement Phase
                        - Design new assertions to improve coverage
                        - Implement assertions in test file
                        
                        ### 3. Verification Phase
                        - Execute project build
                          - Fix compilation errors, max 3 attempts
                          - Reset file if build fails after 3 attempts
                        - Evaluate updated mutation score
                          - Move to next file if score reaches 100%
                          - Move to next file after 3 attempts
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
                    println("> Agent tool called: tool ${tool.name}, args $toolArgs")
                }

                onAgentFinished = { _: String, result: String? ->
                    println("> Agent Result: $result")
                }
            }
        }

        runBlocking {
            agent.run(
                """
                # Project Details:
                - Programming Language: ${projectConfiguration.language}
                - Build System: ${projectConfiguration.buildTool}
                - Source Location: ${projectConfiguration.sourceDir}
                
                # Target Scope:
                - Classes under test: ${projectConfiguration.targetClasses.joinToString(", ")}
                - Test classes: ${projectConfiguration.targetTests.joinToString(", ")}
                """.trimIndent()
            )
        }
    }
}