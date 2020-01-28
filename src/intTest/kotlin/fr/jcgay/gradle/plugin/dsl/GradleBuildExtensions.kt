package fr.jcgay.gradle.plugin.dsl

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun String.dir(rootDirectory: Path): GradleBuildStructure {
    return GradleBuildStructure(this, rootDirectory)
}

class GradleBuildStructure(val content: String, val rootDirectory: Path) {

    private var category: SourceCategory = SourceCategory.BUILD_FILE
    private lateinit var fileName: String

    fun name(fileName: String): GradleBuildStructure {
        this.fileName = fileName
        return this
    }

    fun into(category: SourceCategory): GradleBuildStructure {
        this.category = category
        return this
    }

    fun write() {
        val destination = rootDirectory.resolve(category.path)
        if (Files.notExists(destination)) {
            Files.createDirectories(destination)
        }
        Files.write(destination.resolve(fileName), content.toByteArray(StandardCharsets.UTF_8), StandardOpenOption.CREATE)
    }
}

enum class SourceCategory(val path: String) {
    BUILD_FILE("."),
    MAIN_JAVA("src/main/java"),
    INT_TEST_JAVA("src/intTest/java"),
    UNIT_TEST_JAVA("src/test/java")
}