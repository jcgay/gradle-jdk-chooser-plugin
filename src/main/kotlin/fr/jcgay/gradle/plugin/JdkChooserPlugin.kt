package fr.jcgay.gradle.plugin

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import java.nio.file.Path
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
                configureJavaCompilation(project)

                val expectedJavaVersion = findExpectedJavaVersion(project) ?: return@withPlugin
                configureJavaExecution(project, expectedJavaVersion)
                configureJavaTest(project, expectedJavaVersion)
            }
        }
    }

    private fun configureJavaTest(project: Project, expectedJavaVersion: JavaVersion) {
        project.tasks.withType(Test::class.java) { task ->
            if (JavaVersion.current() != expectedJavaVersion) {
                getInstallation(project, expectedJavaVersion)?.let {
                    project.logger.debug("{} is not the expected Java version {}. Change executable for: {}",
                            JavaVersion.current(), expectedJavaVersion, it)
                    task.executable = "$it/bin/java"
                }
            }
        }
    }

    private fun configureJavaExecution(project: Project, expectedJavaVersion: JavaVersion) {
        project.tasks.withType(JavaExec::class.java) { task ->
            if (JavaVersion.current() != expectedJavaVersion) {
                getInstallation(project, expectedJavaVersion)?.let {
                    project.logger.debug("{} is not the expected Java version {}. Change executable for: {}",
                            JavaVersion.current(), expectedJavaVersion, it)
                    task.executable = "$it/bin/java"
                }
            }
        }
    }

    private fun configureJavaCompilation(project: Project) {
        project.tasks.withType(JavaCompile::class.java) {
            val expectedJavaVersion = JavaVersion.toVersion(it.targetCompatibility)
            if (JavaVersion.current() != expectedJavaVersion) {
                val installation = getInstallation(project, expectedJavaVersion)
                if (installation != null) {
                    project.logger.debug("{} is not the expected Java version {}. Will fork Java compilation with JDK: {}",
                            JavaVersion.current(), expectedJavaVersion, installation)
                    it.options.isFork = true
                    it.options.forkOptions.javaHome = project.file(installation)
                } else if (JavaVersion.current() >= JavaVersion.VERSION_1_9) {
                    it.options.compilerArgs.addAll(listOf("--release", expectedJavaVersion.majorVersion))
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

    private fun getInstallation(project: Project, expectedJavaVersion: JavaVersion): Path? =
            jdkProviders.filter(onlyChosenProvider)
                    .map { it.findInstallation(expectedJavaVersion, project) }
                    .firstOrNull { it != null }

}