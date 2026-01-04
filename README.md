# FracturedJson-Kotlin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.fracturedjson/fractured-json-kotlin.svg)](https://central.sonatype.com/artifact/io.github.fracturedjson/fractured-json-kotlin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0+-blue.svg?logo=kotlin)](http://kotlinlang.org)

A Kotlin port of [FracturedJson](https://github.com/j-brooke/FracturedJson) — a JSON formatter that produces human-readable output with smart line breaks, table-like alignment, and optional comment support.

## Features

- **Smart Formatting**: Automatically chooses between inline, compact, and expanded layouts
- **Table Alignment**: Aligns similar structures like table columns for improved readability
- **Comment Support**: Preserves `//` and `/* */` comments (JSONC format)
- **Multiple Adapters**: Works with kotlinx.serialization, Jackson, or built-in parser
- **Customizable**: Extensive options for line length, indentation, alignment, and more

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.fracturedjson:fractured-json-kotlin:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.github.fracturedjson:fractured-json-kotlin:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.fracturedjson</groupId>
    <artifactId>fractured-json-kotlin</artifactId>
    <version>1.0.0</version>
</dependency>
```

> **Note**: This library includes support for both Jackson and kotlinx.serialization. All adapters are bundled in a single artifact.

## Usage

### With kotlinx.serialization

```kotlin
import io.github.fracturedjson.kotlinx.reformatJson
import io.github.fracturedjson.kotlinx.toFracturedJson
import io.github.fracturedjson.core.FracturedJsonOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

// Reformat a JSON string
val input = """{"name":"Alice","age":30,"scores":[95,87,92]}"""
val formatted = input.reformatJson()
println(formatted)

// Format a JsonElement
val element: JsonElement = Json.parseToJsonElement(input)
val output = element.toFracturedJson()

// With custom options
val options = FracturedJsonOptions(
    maxTotalLineLength = 80,
    indentSpaces = 2
)
val customFormatted = input.reformatJson(options)
```

### With Jackson

```kotlin
import io.github.fracturedjson.jackson.toFracturedJson
import io.github.fracturedjson.jackson.formatJson
import io.github.fracturedjson.core.FracturedJsonOptions
import com.fasterxml.jackson.databind.ObjectMapper

val mapper = ObjectMapper()
val input = """{"name":"Alice","age":30,"scores":[95,87,92]}"""

// Format a JsonNode
val node = mapper.readTree(input)
val formatted = node.toFracturedJson()

// Format via ObjectMapper extension
val output = mapper.formatJson(input)
```

### With Built-in Parser (Comment Support)

```kotlin
import io.github.fracturedjson.parser.Parser
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.CommentPolicy

val input = """
{
    // User information
    "name": "Alice",
    "age": 30,  /* years old */
    "scores": [95, 87, 92]
}
"""

val options = FracturedJsonOptions(
    commentPolicy = CommentPolicy.Preserve
)

val parser = Parser(options)
val items = parser.parse(input)

val formatter = Formatter(options)
val formatted = formatter.format(items)
println(formatted)
```

### Minify JSON

```kotlin
import io.github.fracturedjson.kotlinx.minifyJson

val input = """
{
    "name": "Alice",
    "age": 30
}
"""
val minified = input.minifyJson()
// Output: {"name":"Alice","age":30}
```

## Example Output

Input:
```json
{"name":"Alice","age":30,"address":{"city":"Seoul","zip":"12345"},"scores":[95,87,92,88,91]}
```

Output with default options:
```json
{
    "name": "Alice",
    "age":  30,
    "address": { "city": "Seoul", "zip": "12345" },
    "scores":  [95, 87, 92, 88, 91]
}
```

Table-like alignment for arrays of objects:
```json
{
    "users": [
        { "name": "Alice",   "age": 30, "city": "Seoul"  },
        { "name": "Bob",     "age": 25, "city": "Busan"  },
        { "name": "Charlie", "age": 35, "city": "Daegu"  }
    ]
}
```

## Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `maxTotalLineLength` | Int | 120 | Maximum line length when multiple values share a line |
| `maxInlineComplexity` | Int | 2 | Nesting depth allowed for single-line arrays/objects |
| `maxCompactArrayComplexity` | Int | 2 | Nesting depth for multi-row, multi-item array formatting |
| `maxTableRowComplexity` | Int | 2 | Nesting depth for table-formatted array rows |
| `indentSpaces` | Int | 4 | Spaces per indent level |
| `useTabToIndent` | Boolean | false | Use tabs instead of spaces for indentation |
| `jsonEolStyle` | EolStyle | Default | Line break style (Lf, Crlf, or Default) |
| `nestedBracketPadding` | Boolean | true | Add spaces inside brackets for nested structures |
| `simpleBracketPadding` | Boolean | false | Add spaces inside brackets for simple values |
| `colonPadding` | Boolean | true | Include space after property colons |
| `commaPadding` | Boolean | true | Include space after commas |
| `commentPolicy` | CommentPolicy | TreatAsError | How to handle comments (TreatAsError, Remove, Preserve) |
| `preserveBlankLines` | Boolean | false | Retain blank lines from input |
| `allowTrailingCommas` | Boolean | false | Permit trailing commas in arrays/objects |
| `numberListAlignment` | NumberListAlignment | Decimal | Number alignment style (Left, Right, Decimal) |

## Package Structure

The library is organized into the following packages:

| Package | Description |
|---------|-------------|
| `io.github.fracturedjson.core` | Core formatting engine and configuration options |
| `io.github.fracturedjson.core.formatting` | Internal formatting utilities (buffers, templates) |
| `io.github.fracturedjson.parser` | Built-in JSON parser with comment support |
| `io.github.fracturedjson.parser.tokenizing` | Tokenizer for JSON parsing |
| `io.github.fracturedjson.jackson` | Jackson `JsonNode` integration and extensions |
| `io.github.fracturedjson.kotlinx` | kotlinx.serialization `JsonElement` integration |

## Related Projects

- [FracturedJson](https://github.com/j-brooke/FracturedJson) - Original C# implementation
- [FracturedJson-rs](https://github.com/fcoury/fracturedjson-rs) - Rust port
- [FracturedJsonJs](https://www.npmjs.com/package/fracturedjsonjs) - JavaScript/npm package
- [VS Code Extension](https://marketplace.visualstudio.com/items?itemName=j-brooke.fracturedjsonvsc) - Visual Studio Code extension

## License

MIT License - see [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
