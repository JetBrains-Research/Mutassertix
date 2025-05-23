plugins {
    id("java")
    kotlin("jvm")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation(kotlin("stdlib-jdk8"))

    implementation("ai.koog:koog-agents:0.1.0")

    // Pitest dependencies
    testImplementation("org.hamcrest:hamcrest:3.0")
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
    implementation("org.opentest4j:opentest4j:1.3.0")
}

tasks.register("copyDependencies", Copy::class) {
    group = "build"

    val outputDir = "/Users/arkadii.sapozhnikov/Desktop/Mutassertix/build/libs"

    from(configurations.runtimeClasspath.get().files.map { it })

    into(outputDir)
}

tasks.named("build").configure {
    dependsOn("copyDependencies")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
