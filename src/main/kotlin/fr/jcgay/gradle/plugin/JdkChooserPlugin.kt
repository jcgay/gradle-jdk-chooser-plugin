package fr.jcgay.gradle.plugin

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import java.util.ServiceLoader


class JdkChooserPlugin: Plugin<Project> {

    val jdkProviders = ServiceLoader.load(JdkProvider::class.java)

    override fun apply(project: Project) {
        project.afterEvaluate {
            project.pluginManager.withPlugin("java") {

                project.tasks.withType(JavaCompile::class.java) {
                    val expectedJavaVersion = JavaVersion.toVersion(it.targetCompatibility)
                    if (JavaVersion.current() != expectedJavaVersion) {
                        it.options.isFork = true
                        it.options.forkOptions.javaHome = project.file(getInstallation(project, expectedJavaVersion))
                    }
                }

                val expectedJavaVersion = findExpectedJavaVersion(project) ?: return@withPlugin
                project.tasks.withType(JavaExec::class.java) {
                    if (JavaVersion.current() != expectedJavaVersion) {
                        it.executable = "${getInstallation(project, expectedJavaVersion)}/bin/java"
                    }
                }

                project.tasks.withType(Test::class.java) {
                    if (JavaVersion.current() != expectedJavaVersion) {
                        it.executable = "${getInstallation(project, expectedJavaVersion)}/bin/java"
                    }
                }
            }
        }
    }

    private fun findExpectedJavaVersion(project: Project): JavaVersion? {
        val expectedJavaVersions = HashSet<String>()
        project.tasks.withType(JavaCompile::class.java) {
            expectedJavaVersions.add(it.targetCompatibility)
        }

        if (expectedJavaVersions.size > 1) {
            return null
        }

        return JavaVersion.toVersion(expectedJavaVersions.first())
    }

    private fun getInstallation(project: Project, expectedJavaVersion: JavaVersion): String =
            jdkProviders.map { it.findInstallation(expectedJavaVersion, project) }
                    .firstOrNull { it != null }.toString()

}