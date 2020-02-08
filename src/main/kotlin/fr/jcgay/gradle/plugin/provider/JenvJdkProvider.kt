package fr.jcgay.gradle.plugin.provider

import fr.jcgay.gradle.plugin.JdkProvider
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class JenvJdkProvider(private var home: Path) : JdkProvider {

    constructor(): this(File("${System.getProperty("user.home")}/.jenv/versions").toPath())

    override fun findInstallation(version: JavaVersion, project: Project): Path? {
        if (!Files.exists(home)) {
            return null
        }

        return Files.list(home)
                .sorted()
                .filter { JavaVersion.toVersion(it.fileName) == version }
                .findFirst()
                .orElse(null)
    }
}