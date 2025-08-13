package ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.markdown.markdown
import org.jetbrains.research.mutassertix.agent.PrivateAgentUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

/**
 * An agent that applies a mutation to a Java class.
 *
 * This agent takes a Java class file path and a mutation description as a string,
 * applies the mutation to the Java class, and returns the updated Java code.
 * The output file is created in the same folder as the input file and is removed after the method completes.
 */
class MutationApplyingAgent(executor: PromptExecutor) {
    private val logger: Logger = LoggerFactory.getLogger(MutationApplyingAgent::class.java)
    private val toolRegistry = PublicAgentUtils.getToolRegistry()

    private val strategy = PublicAgentUtils.createCommonStrategy("mutation-applying-agent-strategy")

    private val agentConfig = AIAgentConfig(
        prompt = prompt("mutation-applying-agent-prompt") {
            system {
                markdown {
                    +"You are an intelligent mutation applying agent for Java code."
                    +"Your task is to apply a mutation to a Java class based on a mutation description."
                    +"You will receive a path to a Java class file and a mutation description as a string."
                    +"You must read the Java code from the file, apply the mutation, and write the updated Java code to a specified output file."
                    
                    h1("PROCESS OVERVIEW")
                    numbered {
                        item("Parse the mutation description to extract the target class, line number, location, and mutation name")
                        item("Read the Java code from the input file using the readTextFromFile tool")
                        item("Locate the specific line and code element to be mutated")
                        item("Apply the mutation based on the mutation name")
                        item("Verify the mutation was applied correctly and doesn't introduce syntax errors")
                        item("Write the updated Java code to the output file using the writeTextToFile tool")
                    }
                    
                    h1("MUTATION DESCRIPTION FORMAT")
                    +"The mutation description has the following format:"
                    +"File: [target class], Line: [line number], Location: [location], Mutation: [mutation name]"
                    +"Where:"
                    bulleted {
                        item("[target class] is the fully qualified name of the class being mutated")
                        item("[line number] is the line number where the mutation occurs")
                        item("[location] is the specific location of the mutation (e.g., a method or field name)")
                        item("[mutation name] is the name of the mutation (e.g., the type of mutation applied)")
                    }
                }
            }
        },
        model = PrivateAgentUtils.getGPT4Model(),
        maxAgentIterations = 500
    )

    private val agent = AIAgent(
        promptExecutor = executor,
        strategy = strategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry
    ) {
        handleEvents {
            CustomEventHandler.create()()
        }
    }

    /**
     * Applies a mutation to a Java class.
     *
     * @param originalCodeFilePath The path to the Java class file.
     * @param mutationDescription The mutation description as a string.
     * @return The updated Java code with the mutation applied, or null if an error occurred.
     *         The output file is created in the same folder as the input file and is removed after the method completes.
     */
    suspend fun applyMutation(originalCodeFilePath: File, mutationDescription: String): String? {
        if (!originalCodeFilePath.exists()) {
            logger.error("Input file does not exist: {}", originalCodeFilePath.absolutePath)
            return null
        }

        // Create an output file in the same folder as the input file
        val parentDir = originalCodeFilePath.parentFile
        val outputFileName = "mutated_java_${UUID.randomUUID()}.java"
        val outputFilePath = "${parentDir.absolutePath}/$outputFileName"
        val outputFile = File(outputFilePath)

        try {
            agent.run(
                markdown {
                    h1("Java Class")
                    +"The original Java code is available in the file: ${originalCodeFilePath.absolutePath}"
                    +"You can read it using the readTextFromFile tool."

                    h1("Mutation Description")
                    +mutationDescription
                    
                    h1("Output File")
                    +"Write the updated Java code to this file: $outputFilePath"
                    +"Use the writeTextToFile tool to write the updated code."
                }
            )

            if (outputFile.exists()) {
                val updatedCode = outputFile.readText()
                outputFile.delete()
                return updatedCode
            } else {
                logger.error("Output file was not created: {}", outputFilePath)
                return null
            }
        } catch (e: Exception) {
            logger.error("Error in MutationApplyingAgent: {}", e.message, e)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return null
        }
    }
}
