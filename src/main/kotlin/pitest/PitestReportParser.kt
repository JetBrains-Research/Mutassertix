package pitest

class PitestReportParser {
    fun getMutationScore(output: String): Int {
        val regex = """>> Generated \d+ mutations Killed \d+ \((\d+)%\)""".toRegex()
        return regex.find(output)?.groupValues?.get(1)?.toInt() ?: 0
    }
}
