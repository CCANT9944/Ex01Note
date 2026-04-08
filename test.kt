fun main() {
    val text = "Hello \uE000world\uE001abc"
    val nextText = "Hello \uE000world\uE001ab"
    println((text.length - nextText.length) == 1)
}
