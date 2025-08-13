plugins {
    id("java")
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")

        maven {
            val spaceUsername =
                System.getProperty("space.username") ?: project.properties["spaceUsername"]?.toString() ?: ""
            val spacePassword =
                System.getProperty("space.pass") ?: project.properties["spacePassword"]?.toString() ?: ""

            url = uri("https://packages.jetbrains.team/maven/p/testing-agents/assertion-generation-agent")
            credentials {
                username = spaceUsername
                password = spacePassword
            }
        }
    }
}

dependencies {
    val jetbrainsAiVersion = "1.0.0-beta.68+0.4.71"
    val koogVersion = "0.2.1"
    implementation("ai.jetbrains.code.prompt:code-prompt-executor-grazie-koog:$jetbrainsAiVersion")
    implementation("ai.jetbrains.code.agents:code-agents-micro-base:$jetbrainsAiVersion")
    implementation("ai.koog:koog-agents:$koogVersion")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.research:assertion-generation-agent:1.1.1")

    // Logging dependencies
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("ch.qos.logback:logback-core:1.4.14")

    implementation("org.hibernate:hibernate-core:5.5.0.Beta1")
    testImplementation("junit:junit:4.+")

    // Pitest dependencies
    testImplementation("org.hamcrest:hamcrest:3.0")
    implementation("org.testng:testng:7.11.0")
    implementation("org.mockito:mockito-core:5.18.0")
    implementation("org.mockito:mockito-junit-jupiter:5.18.0")
    implementation("org.pitest:pitest-command-line:1.19.1")
    implementation("org.pitest:pitest-entry:1.19.1")
    implementation("org.pitest:pitest:1.19.1")
    implementation("org.pitest:pitest-junit5-plugin:1.2.2")
    implementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
    implementation("junit:junit:4.13.2")
    implementation("org.junit.platform:junit-platform-commons:1.12.2")
    implementation("org.junit.platform:junit-platform-launcher:1.12.2")
    implementation("org.junit.platform:junit-platform-engine:1.12.2")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.12.2")
    implementation("org.junit.platform:junit-platform-console:1.12.2")
    implementation("org.opentest4j:opentest4j:1.3.0")
    api("com.google.guava:guava:32.0.1-android")

    // ProGuard dependencies
    implementation("org.benf:cfr:0.152")
    implementation("com.guardsquare:proguard-gradle:7.7.0")

    // JavaParser dependencies
    implementation("com.github.javaparser:javaparser-core:3.27.0")
}

tasks.register("copyDependencies", Copy::class) {
    group = "build"

    val outputDir = layout.buildDirectory.dir("libs").get().asFile.absolutePath

    from(configurations.runtimeClasspath.get().files.map { it })

    into(outputDir)
}

tasks.named("build").configure {
    dependsOn("copyDependencies")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("extractTargetPairs") {
    group = "application"
    description = "Extract target pairs from java.json and compare initial and final versions"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ExtractTargetPairsKt")
}

tasks.register<JavaExec>("analyzeCodeQuality") {
    group = "application"
    description = "Analyze code quality of generated test files"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("CodeQualityAnalyzerKt")
}

kotlin {
    jvmToolchain(17)
}
