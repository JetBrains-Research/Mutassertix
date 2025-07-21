package ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.markdown.markdown
import data.PropertiesReader
import org.jetbrains.research.mutassertix.agent.AgentUtils

object AssertionGenerationAgent {
    val executor = AgentUtils.getExecutor(PropertiesReader.grazieToken)

    val toolRegistry = ToolRegistry {
        tools(Tools().asTools())
    }

    val strategy = strategy("assertion-generation-agent-strategy") {
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
        prompt = prompt("assertion-generation-agent-prompt") {
            system {
                markdown {
                    +"You are an intelligent assertion generator for language Java designed to improve project mutation scores."
                    +"You have to improve the assertion implementation of existing test files. Do not add tests for new functionality."
                    +"Use survived mutations data to get missed cases."
                }
            }
        },
        model = AgentUtils.getSonnet_4(),
        maxAgentIterations = 500
    )

    val agent = AIAgent(
        promptExecutor = executor,
        strategy = strategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry
    )

    suspend fun run(prompt: String) = agent.run(prompt)
}
