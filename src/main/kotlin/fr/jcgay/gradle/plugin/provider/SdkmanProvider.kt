package fr.jcgay.gradle.plugin.provider

import fr.jcgay.gradle.plugin.JdkProvider
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class SdkmanProvider(private val home: Path) : JdkProvider {

    constructor(): this(File("${System.getProperty("user.home")}/.sdkman/candidates/java").toPath())

    override fun findInstallation(version: JavaVersion, project: Project): Path? {
        if (!Files.exists(home)) {
            return null
        }

        return Files.list(home)
                .filter { it.fileName.toString() != "current" }
                .sorted(reverseOrder())
                .filter { JavaVersion.toVersion(it.fileName) == version }
                .findFirst()
                .orElse(null)
    }
}