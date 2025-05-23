package ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.ext.tool.AskUser
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.local.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import data.ConfigReader
import kotlinx.coroutines.runBlocking

class Agent {
    fun run(filePath: String) = runBlocking {
        val executor: PromptExecutor = simpleOpenAIExecutor(ConfigReader().openAIToken)

        val toolRegistry = ToolRegistry {
            tool(AskUser)
            tool(SayToUser)
            tools(Tools().asTools())
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
                system("You are a file content manager.")
            },
            model = OpenAIModels.Chat.GPT4o,
            maxAgentIterations = 50
        )

        val agent = AIAgent(
            promptExecutor = executor,
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = toolRegistry
        ) {
            handleEvents {
                onToolCall = { tool: Tool<*, *>, toolArgs: Tool.Args ->
                    println("> Tool called: tool ${tool.name}, args $toolArgs")
                }

                onAgentRunError = { _: String, throwable: Throwable ->
                    println("> ERROR: ${throwable.message}\n${throwable.stackTraceToString()}")
                }

                onAgentFinished = { _: String, result: String? ->
                    println("> Result: $result")
                }
            }
        }

        runBlocking {
            agent.run("Remove all code in $filePath, and add here hello world implementation")
        }
    }
}