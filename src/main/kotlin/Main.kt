import pitest.PitestPipeline

fun main() {
//    val mutationScore = PitestPipeline().getMutationScore(
//        "/Users/arkadii.sapozhnikov/Desktop/untitled",
//        listOf(
//            "build/classes/java/main",
//            "build/classes/java/test"
//        ), "Calculator", "CalculatorTest"
//    )

    val mutationScore = PitestPipeline().getMutationScore(
        "/Users/arkadii.sapozhnikov/Desktop/nbvcxz",
        listOf(
            "target/classes",
            "target/test-classes"
        ), "me.gosimple.nbvcxz.Nbvcxz", "me.gosimple.nbvcxz.NbvcxzTest"
    )

    println(mutationScore)
}
