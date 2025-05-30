package data

import java.util.Properties
import java.io.FileInputStream

class ConfigReader {
    private val properties = Properties()

    init {
        val inputStream = FileInputStream("src/main/resources/Config.properties")
        properties.load(inputStream)
        inputStream.close()
    }

    val javaPath: String
        get() = properties.getProperty("javaPath") ?: ""

    val grazieToken: String
        get() = properties.getProperty("grazieToken") ?: ""
}