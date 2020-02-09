package fr.jcgay.gradle.plugin

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import java.util.ServiceLoader


class JdkChooserPlugin: Plugin<Project> {

    private val jdkProviders = ServiceLoader.load(JdkProvider::class.java)
    private var onlyChosenProvider: (JdkProvider) -> Boolean = { true }

    override fun apply(project: Project) {
        val extension = project.extensions.create("jdk", JdkChooserExtension::class.java)

        project.afterEvaluate {
            if (extension.provider != null) {
                onlyChosenProvider = { it.javaClass.simpleName == extension.provider }
            }

            project.pluginManager.withPlugin("java") {

                project.tasks.withType(JavaCompile::class.java) {
                    val expectedJavaVersion = JavaVersion.toVersion(it.targetCompatibility)
                    if (JavaVersion.current() != expectedJavaVersion) {
                        val installation = getInstallation(project, expectedJavaVersion)
                        project.logger.debug("{} is not the expected Java version {}. Will fork Java compilation with JDK: {}",
                                JavaVersion.current(), expectedJavaVersion, installation)
                        it.options.isFork = true
                        it.options.forkOptions.javaHome = project.file(installation)
                    }
                }

                val expectedJavaVersion = findExpectedJavaVersion(project) ?: return@withPlugin

                project.tasks.withType(JavaExec::class.java) {
                    if (JavaVersion.current() != expectedJavaVersion) {
                        val matchingInstallation = getInstallation(project, expectedJavaVersion)
                        project.logger.debug("{} is not the expected Java version {}. Change executable for: {}",
                                JavaVersion.current(), expectedJavaVersion, matchingInstallation)
                        it.executable = "$matchingInstallation/bin/java"
                    }
                }

                project.tasks.withType(Test::class.java) {
                    if (JavaVersion.current() != expectedJavaVersion) {
                        val matchingInstallation = getInstallation(project, expectedJavaVersion)
                        project.logger.debug("{} is not the expected Java version {}. Change executable for: {}",
                                JavaVersion.current(), expectedJavaVersion, matchingInstallation)
                        it.executable = "$matchingInstallation/bin/java"
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
            jdkProviders.filter(onlyChosenProvider)
                    .map { it.findInstallation(expectedJavaVersion, project) }
                    .firstOrNull { it != null }.toString()

}