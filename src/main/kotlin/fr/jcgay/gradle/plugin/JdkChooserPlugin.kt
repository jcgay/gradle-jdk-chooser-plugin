package fr.jcgay.gradle.plugin

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.process.JavaForkOptions
import java.nio.file.Path
import java.util.*


class JdkChooserPlugin: Plugin<Project> {

    private val jdkProviders = ServiceLoader.load(JdkProvider::class.java)

    override fun apply(project: Project) {
        val extension = project.extensions.create("jdk", JdkChooserExtension::class.java)

        project.pluginManager.withPlugin("java") {
            configureJavaCompilation(project, extension)
            configureJavaExecution(project, extension)
            configureJavaTest(project, extension)
        }
    }

    private fun configureJavaTest(project: Project, extension: JdkChooserExtension) {
        project.tasks.withType(Test::class.java).configureEach { task ->
            task.doFirst { changeExecutable(project, extension, task) }
        }
    }

    private fun changeExecutable(project: Project, extension: JdkChooserExtension, task: JavaForkOptions) {
        findExpectedJavaVersion(project)?.let { expectedJavaVersion ->
            if (JavaVersion.current() != expectedJavaVersion) {
                getInstallation(project, expectedJavaVersion, extension)?.let {
                    project.logger.debug("{} is not the expected Java version {}. Change executable for: {}",
                            JavaVersion.current(), expectedJavaVersion, it)
                    task.executable = "$it/bin/java"
                }
            }
        }
    }

    private fun configureJavaExecution(project: Project, extension: JdkChooserExtension) {
        project.tasks.withType(JavaExec::class.java).configureEach { task ->
            task.doFirst { changeExecutable(project, extension, task) }
        }
    }

    private fun configureJavaCompilation(project: Project, extension: JdkChooserExtension) {
        project.tasks.withType(JavaCompile::class.java).configureEach {task ->
            task.doFirst {
                val expectedJavaVersion = JavaVersion.toVersion(task.targetCompatibility)
                if (JavaVersion.current() != expectedJavaVersion) {
                    val installation = getInstallation(project, expectedJavaVersion, extension)
                    if (installation != null) {
                        project.logger.debug("{} is not the expected Java version {}. Will fork Java compilation with JDK: {}",
                                JavaVersion.current(), expectedJavaVersion, installation)
                        task.options.isFork = true
                        task.options.forkOptions.javaHome = project.file(installation)
                    } else if (JavaVersion.current() >= JavaVersion.VERSION_1_9) {
                        task.options.compilerArgs.addAll(listOf("--release", expectedJavaVersion.majorVersion))
                    }
                }
            }
        }
    }

    private fun findExpectedJavaVersion(project: Project): JavaVersion? {
        return project.extensions.findByType(JavaPluginExtension::class.java)?.targetCompatibility
    }

    private fun getInstallation(project: Project, expectedJavaVersion: JavaVersion, extension: JdkChooserExtension): Path? =
            jdkProviders.filter { it.javaClass.simpleName == extension.provider }
                    .map { it.findInstallation(expectedJavaVersion, project) }
                    .firstOrNull { it != null }

}