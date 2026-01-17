import io.github.fracturedjson.kotlinx.reformatJson
import io.github.fracturedjson.core.FracturedJsonOptions
import java.io.File

fun main() {
    val options = FracturedJsonOptions(
        maxTotalLineLength = 100,
        indentSpaces = 2
    )
    
    println("=== API Logs (Array) ===")
    val logsJson = File("samples/api-logs.json").readText()
    println(logsJson.reformatJson(options))
    
    println("\n=== Complex API Response ===")
    val complexJson = File("samples/complex-api-response.json").readText()
    println(complexJson.reformatJson(options))
}
