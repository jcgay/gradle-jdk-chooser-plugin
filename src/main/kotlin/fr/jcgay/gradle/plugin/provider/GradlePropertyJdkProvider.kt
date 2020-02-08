package fr.jcgay.gradle.plugin.provider

import fr.jcgay.gradle.plugin.JdkProvider
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.io.File
import java.nio.file.Path


class GradlePropertyJdkProvider: JdkProvider {

    override fun findInstallation(version: JavaVersion, project: Project): Path? {
        val installation = project.findProperty("installation.jdk.${version.majorVersion}")
        return if (installation == null) null else File(installation.toString()).toPath()
    }
}