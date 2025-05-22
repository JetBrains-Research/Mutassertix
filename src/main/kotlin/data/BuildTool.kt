package data

interface BuildTool {
    val name: String
    val projectDependencies: List<String>
    val buildCommand: String
}
