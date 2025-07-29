package ai

import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import org.jetbrains.research.mutassertix.agent.PrivateAgentUtils

object PublicAgentUtils {
    fun getToolRegistry(): ToolRegistry = ToolRegistry {
        tools(Tools().asTools())
    } + PrivateAgentUtils.getThinkTool()

    /**
     * Creates a common strategy pattern used by all agents.
     * 
     * @param strategyName The name of the strategy.
     * @return A strategy with the provided name and common node and edge definitions.
     */
    fun createCommonStrategy(strategyName: String) = strategy(strategyName) {
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
}
