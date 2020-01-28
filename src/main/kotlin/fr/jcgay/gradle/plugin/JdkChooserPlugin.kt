package fr.jcgay.gradle.plugin

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile


class JdkChooserPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        project.afterEvaluate {
            project.pluginManager.withPlugin("java") {
                project.tasks.withType(JavaCompile::class.java) {
                    val expectedJavaVersion = JavaVersion.toVersion(it.targetCompatibility)
                    if (JavaVersion.current() != expectedJavaVersion) {
                        it.options.isFork = true
                        it.options.forkOptions.javaHome = project.file(
                                project.property("installation.jdk.${expectedJavaVersion.majorVersion}")
                                        ?: throw IllegalStateException("Cannot find a valid JDK installation for ${expectedJavaVersion.majorVersion}"))
                    }
                }
            }
        }
    }
}