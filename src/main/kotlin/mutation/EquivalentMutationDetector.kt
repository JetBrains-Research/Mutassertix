package mutation

import ai.MutationApplyingAgent
import ai.koog.prompt.executor.model.PromptExecutor
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.visitor.NoCommentEqualsVisitor
import data.ProjectConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object EquivalentMutationDetector {
    private val logger: Logger = LoggerFactory.getLogger(EquivalentMutationDetector::class.java)

    fun detect(
        projectConfig: ProjectConfig,
        executor: PromptExecutor,
        codeFilePath: File,
        mutationDescription: String
    ): Boolean {
        logger.info(
            "Starting mutation equivalence detection for file: {}, mutation: {}",
            codeFilePath.name,
            mutationDescription
        )

        logger.info("Applying mutation...")
        val mutantCode = runBlocking {
            MutationApplyingAgent(executor).applyMutation(codeFilePath, mutationDescription)
        }

        if (mutantCode == null) {
            logger.warn("Failed to apply mutation, mutation agent returned null")
            return false
        }
        logger.debug("Mutation applied successfully, mutant code length: {} characters", mutantCode.length)

        // Unsuccessful to change the code
        val initialCode = codeFilePath.readText()
        if (initialCode == mutantCode) return false

        val result = detectCodeEquivalence(projectConfig, codeFilePath, initialCode, mutantCode)
        logger.info("Mutation equivalence detection result: {}", if (result) "EQUIVALENT" else "NOT EQUIVALENT")
        return result
    }

    private fun detectCodeEquivalence(
        projectConfig: ProjectConfig,
        codeFilePath: File,
        initialCode: String,
        mutantCode: String
    ): Boolean {
        logger.debug("Detecting code equivalence between initial code and mutant code")

        val transformedMutantCode =
            applyProGuardTransformations(projectConfig, codeFilePath, mutantCode)

        logger.debug("Applying ProGuard transformations to initial code")
        val transformedInitialCode =
            applyProGuardTransformations(projectConfig, codeFilePath, initialCode)

        logger.debug("Checking equality between transformed codes")
        return equalCodeCheck(transformedInitialCode, transformedMutantCode)
    }

    private fun applyProGuardTransformations(
        projectConfig: ProjectConfig,
        codeFilePath: File,
        javaCode: String
    ): String {
        logger.debug("Starting ProGuard transformations on Java code")

        codeFilePath.writeText(javaCode)
        val fileDirFile = codeFilePath.parentFile
        val fileDirPath = Path(fileDirFile.path)

        val inputDir = Files.createDirectory(fileDirPath.resolve("input"))
        val outputDir = Files.createDirectory(fileDirPath.resolve("output"))
        val configFile = fileDirPath.resolve("proguard.config")

        try {
            // Extract the package name from the code
            logger.debug("Extracting package name from code")
            val packageRegex = "package\\s+([\\w.]+)".toRegex()
            val packageMatch = packageRegex.find(javaCode)
            val packageName = packageMatch?.groupValues?.get(1) ?: ""
            logger.debug("Extracted package name: {}", packageName.ifEmpty { "<empty>" })

            // Extract the class name from the code
            logger.debug("Extracting class name from code")
            val classRegex = "class\\s+(\\w+)".toRegex()
            val classMatch = classRegex.find(javaCode)
            if (classMatch == null) {
                logger.error("Failed to extract class name from code")
                return "Error: Could not extract class name from code"
            }
            val actualClassName = classMatch.groupValues[1]
            logger.debug("Extracted class name: {}", actualClassName)

            // Compile the Java file with the classpath set to the input directory
            logger.info("Compiling Java file: {}", codeFilePath)
            val compileProcess = ProcessBuilder(
                "javac",
                "-d",
                inputDir.toString(),
                "-cp",
                buildClasspath(projectConfig),
                codeFilePath.toString()
            )
                .redirectErrorStream(true)
                .start()

            val compileOutput = BufferedReader(InputStreamReader(compileProcess.inputStream)).readText()
            compileProcess.waitFor()

            if (compileProcess.exitValue() != 0) {
                logger.error("Compilation failed: {}", compileOutput)
                return "Compilation failed: $compileOutput"
            }
            logger.debug("Compilation successful")

            // Prepare the config file
            logger.debug("Preparing ProGuard configuration file")

            val configContent = """
                -injars ${inputDir.toAbsolutePath()}
                -outjars ${outputDir.toAbsolutePath()}
            
                # Suppress warnings about unresolved references
                -dontwarn
                -dontnote
            
                -dontusemixedcaseclassnames
                -dontskipnonpubliclibraryclasses
                -verbose
            
                # Keep the main class and its main method
                -keep public class * {
                    public static void main(java.lang.String[]);
                }
            
                # Keep the StringBuilder2 class for demonstration
                -keep class ${packageName}.${actualClassName} {
                    *;
                }
                
                # Strongest configuration combining aggressive obfuscation and optimization
                
                # Maximum optimization
                -optimizationpasses 10
                -optimizations *
                
                # Aggressive obfuscation
                -overloadaggressively
                -repackageclasses '${packageName}'
                -allowaccessmodification
                -adaptclassstrings
                -adaptresourcefilenames
                -adaptresourcefilecontents
                
                # Additional aggressive options
                -mergeinterfacesaggressively
                -useuniqueclassmembernames
                -dontusemixedcaseclassnames
                -flattenpackagehierarchy '${packageName}'
            """.trimIndent()

            Files.write(configFile, configContent.toByteArray())
            logger.debug("ProGuard configuration file created at: {}", configFile)

            // Run ProGuard
            logger.info("Running ProGuard")
            val proguardJar = Paths.get("proguard-7.7.0/lib/proguard.jar").toAbsolutePath()
            if (!proguardJar.exists()) {
                logger.error("ProGuard JAR not found at: {}", proguardJar)
                return "ProGuard JAR not found at: $proguardJar"
            }

            val proguardProcess = ProcessBuilder(
                "java", "-jar", proguardJar.toString(), "@${configFile.toAbsolutePath()}"
            ).redirectErrorStream(true).start()

            val proguardOutput = BufferedReader(InputStreamReader(proguardProcess.inputStream)).readText()
            proguardProcess.waitFor()

            if (proguardProcess.exitValue() != 0) {
                logger.error("ProGuard processing failed: {}", proguardOutput)
                return "ProGuard processing failed: $proguardOutput"
            }
            logger.debug("ProGuard processing successful")

            logger.debug("Looking for processed class files in: {}", outputDir)
            val processedClassFiles = Files.walk(outputDir)
                .filter { it.toString().endsWith(".class") }
                .toList()

            if (processedClassFiles.isEmpty()) {
                logger.error("No processed class files found in output directory")
                return "No processed class files found"
            }
            logger.debug("Found {} processed class files", processedClassFiles.size)

            val targetClassFile = if (processedClassFiles.size > 1) {
                logger.debug(
                    "Multiple class files found, searching for the one matching class name: {}",
                    actualClassName
                )
                processedClassFiles.find {
                    it.fileName.toString().startsWith(actualClassName) ||
                            it.fileName.toString().contains(actualClassName, ignoreCase = true)
                } ?: processedClassFiles[0]
            } else {
                processedClassFiles[0]
            }
            logger.debug("Selected target class file: {}", targetClassFile)

            // Use CFR to decompile the class file to Java source code
            logger.info("Decompiling class file to Java source code")
            val classFilePath = targetClassFile.toString()
            val decompiled = decompileWithCFR(classFilePath)

            if (decompiled.startsWith("Error:") || decompiled.startsWith("CFR decompilation failed:")) {
                logger.error("Decompilation failed: {}", decompiled)
            } else {
                logger.debug("Decompilation successful, code length: {} characters", decompiled.length)
            }

            return decompiled
        } catch (e: Exception) {
            logger.error("Error during ProGuard transformations", e)
            return "Error: ${e.message}"
        } finally {
            // Clean up temporary files
            logger.debug("Cleaning up temporary directories and files")
            Files.walk(inputDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
            Files.walk(outputDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
            Files.deleteIfExists(configFile)
            logger.debug("Cleanup completed")
        }
    }

    private fun decompileWithCFR(classFilePath: String): String {
        logger.debug("Starting decompilation of class file: {}", classFilePath)

        val cfrJarPath = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .find { it.contains("cfr") }

        if (cfrJarPath == null) {
            logger.error("CFR JAR not found in classpath")
            return "CFR JAR not found in classpath"
        }
        logger.debug("Found CFR JAR at: {}", cfrJarPath)

        try {
            // Create a temporary directory for the output
            logger.debug("Creating temporary directory for CFR output")
            val tempOutputDir = createTempDirectory("cfr_output")
            logger.debug("Created temporary directory: {}", tempOutputDir)

            // Run CFR as a command-line tool
            logger.info("Running CFR decompiler on class file")
            val process = ProcessBuilder(
                "java",
                "-jar",
                cfrJarPath,
                classFilePath,
                "--outputdir",
                tempOutputDir.toString()
            ).redirectErrorStream(true).start()

            // Capture and log the process output
            val processOutput = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()

            if (process.exitValue() != 0) {
                logger.error("CFR decompilation failed with exit code {}: {}", process.exitValue(), processOutput)
                return "CFR decompilation failed: $processOutput"
            }
            logger.debug("CFR decompilation process completed successfully")

            // Find the decompiled Java file
            logger.debug("Searching for decompiled Java file in: {}", tempOutputDir)
            val decompiled = Files.walk(tempOutputDir)
                .filter { it.toString().endsWith(".java") }
                .findFirst()
                .orElse(null)

            if (decompiled == null) {
                logger.error("No decompiled Java file found in output directory")
                return "No decompiled Java file found"
            }
            logger.debug("Found decompiled Java file: {}", decompiled)

            // Read the decompiled Java code
            logger.debug("Reading decompiled Java code")
            val decompiledCode = Files.readString(decompiled)
            logger.debug("Successfully read decompiled code, length: {} characters", decompiledCode.length)

            // Clean up
            logger.debug("Cleaning up temporary files in: {}", tempOutputDir)
            Files.walk(tempOutputDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }

            return decompiledCode
        } catch (e: Exception) {
            logger.error("Error during decompilation with CFR", e)
            return "Error decompiling with CFR: ${e.message}"
        }
    }

    private fun equalCodeCheck(initialCode: String, mutantCode: String): Boolean {
        logger.debug("Performing equality check between initial code and mutant code")

        logger.debug("Configuring JavaParser")
        val parser = JavaParser()
        parser.parserConfiguration
            .setCharacterEncoding(StandardCharsets.UTF_8).languageLevel =
            com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_8

        logger.debug("Parsing initial code")
        val initialCodeParseResult: com.github.javaparser.ParseResult<com.github.javaparser.ast.CompilationUnit> =
            parser.parse(initialCode)

        if (!initialCodeParseResult.isSuccessful) {
            logger.error("Failed to parse initial code: {}", initialCodeParseResult.problems)
            return false
        }
        logger.debug("Initial code parsed successfully")

        val initialCodeCU: com.github.javaparser.ast.CompilationUnit = initialCodeParseResult.getResult().get()

        logger.debug("Parsing mutant code")
        val mutantCodeParseResult: com.github.javaparser.ParseResult<com.github.javaparser.ast.CompilationUnit> =
            parser.parse(mutantCode)

        if (!mutantCodeParseResult.isSuccessful) {
            logger.error("Failed to parse mutant code: {}", mutantCodeParseResult.problems)
            return false
        }
        logger.debug("Mutant code parsed successfully")

        val mutantCodeCU: com.github.javaparser.ast.CompilationUnit = mutantCodeParseResult.getResult().get()

        logger.debug("Comparing ASTs using NoCommentEqualsVisitor")
        val result = NoCommentEqualsVisitor.equals(initialCodeCU, mutantCodeCU)
        logger.debug("AST comparison result: {}", if (result) "EQUAL" else "NOT EQUAL")

        return result
    }

    private fun buildClasspath(
        projectConfig: ProjectConfig,
    ): String {
        val libFolder = "build/libs"

        val projectDependencies = projectConfig.buildTool.projectDependencies.map { "${projectConfig.sourceDir}/$it" }
        val libraryDependencies =
            projectConfig.libraryDependencies.map { it.split("/").last() }.map { "$libFolder/$it" }

        return (projectDependencies + libraryDependencies).joinToString(":")
    }
}
