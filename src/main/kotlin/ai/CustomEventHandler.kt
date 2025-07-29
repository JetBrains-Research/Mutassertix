package ai

import ai.koog.agents.features.eventHandler.feature.EventHandlerConfig
import kotlin.uuid.ExperimentalUuidApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@OptIn(ExperimentalUuidApi::class)
data object CustomEventHandler {
    private val logger: Logger = LoggerFactory.getLogger(CustomEventHandler::class.java)
    fun create(): EventHandlerConfig.() -> Unit = {
        onToolCall = { tool, args ->
            logger.info("Tool called: {} with args: {}", tool.name, args)
        }

        onToolValidationError = { _, _, error ->
            logger.error("Tool validation error: {}", error)
        }

        onToolCallFailure = { _, _, error ->
            logger.error("Tool call failure: {}", error)
        }

        onAgentRunError = { _, _, exception ->
            logger.error("Agent run error", exception)
        }

        onAgentFinished = { strategyName, result ->
            logger.info("Agent finished: {} with result: {}", strategyName, result)
        }
    }
}
