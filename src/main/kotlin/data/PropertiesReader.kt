package data

import java.util.Properties
import java.io.FileInputStream

object PropertiesReader {
    private fun get(key: String): String {
        val properties = Properties()
        val inputStream = FileInputStream("gradle.properties")
        properties.load(inputStream)
        inputStream.close()
        return properties.getProperty(key) ?: ""
    }

    val javaPath: String = get("javaPath")

    val grazieToken: String = get("grazieToken")
}
