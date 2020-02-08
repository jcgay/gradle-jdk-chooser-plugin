package fr.jcgay.gradle.plugin

import fr.jcgay.gradle.plugin.dsl.SourceCategory.MAIN_JAVA
import fr.jcgay.gradle.plugin.dsl.SourceCategory.UNIT_TEST_JAVA
import fr.jcgay.gradle.plugin.dsl.dir
import fr.jcgay.gradle.plugin.jdk.JdkProvider
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@TestInstance(PER_CLASS)
internal class JdkChooserPluginTest {

    @JvmField
    @RegisterExtension
    val jdks = JdkProvider()
            .withVersion(VERSION_1_8, VERSION_11)
            .atPath(File("build").toPath())

    private val gradleVersion = "6.1.1"

    @Test
    internal fun `should use a different jdk when current runtime is not the one targeted`(@TempDir tempDir: Path) {
        """
            org.gradle.java.home=${jdks.getInstallation(VERSION_1_8).toFile().canonicalPath}
            installation.jdk.11=${jdks.getInstallation(VERSION_11).toFile().canonicalPath}
        """.trimIndent()
                .dir(tempDir).name("gradle.properties").write()

        """
            plugins {
                id 'java'
                id 'fr.jcgay.gradle-jdk-chooser-plugin'
            }
            
            java {
               sourceCompatibility = JavaVersion.VERSION_11
               targetCompatibility = JavaVersion.VERSION_11
            }
            
            tasks.create("validateCurrentJdk") {
                if (JavaVersion.current() != JavaVersion.VERSION_1_8) {
                    throw new TaskExecutionException(it, new IllegalStateException("You should run the build with a JDK 8 😇"))
                }
            }
            
            tasks.compileJava.dependsOn("validateCurrentJdk")
        """.trimIndent()
                .dir(tempDir).name("build.gradle").write()

        """
            public class MyClass {
                public static void main(String... args) {
                    System.out.println(java.util.List.of("Hello", "World!"));
                }
            }
        """.trimIndent()
                .dir(tempDir).name("MyClass.java").into(MAIN_JAVA).write()

        val result: BuildResult = GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withGradleVersion(gradleVersion)
                .withArguments("compileJava")
                .withPluginClasspath()
                .build()

        assertThat(result.task(":compileJava")?.outcome).isEqualTo(SUCCESS)
    }

    @Test
    internal fun `should not fork compilation when current runtime is the one targeted`(@TempDir tempDir: Path) {
        """
            org.gradle.java.home=${jdks.getInstallation(VERSION_1_8).toFile().canonicalPath}
        """.trimIndent()
                .dir(tempDir).name("gradle.properties").write()

        """
            plugins {
                id 'java'
                id 'fr.jcgay.gradle-jdk-chooser-plugin'
            }
            
            java {
               sourceCompatibility = JavaVersion.VERSION_1_8
               targetCompatibility = JavaVersion.VERSION_1_8
            }
            
            tasks.create("validateCurrentJdk") {
                if (JavaVersion.current() != JavaVersion.VERSION_1_8) {
                    throw new TaskExecutionException(it, new IllegalStateException("You should run the build with a JDK 8 😇"))
                }
            }
            
            tasks.compileJava.dependsOn("validateCurrentJdk")
            
            tasks.withType(JavaCompile) {
                if (options.fork) {
                    throw new TaskExecutionException(it, new IllegalStateException("The current JDK version is 8, no need to fork!"))
                }
                if (options.forkOptions.javaHome != null) {
                    throw new TaskExecutionException(it, new IllegalStateException("The current JDK version is 8, no need to define another java home!"))
                }
            }
        """.trimIndent()
                .dir(tempDir).name("build.gradle").write()

        val result: BuildResult = GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withGradleVersion(gradleVersion)
                .withArguments("compileJava")
                .withPluginClasspath()
                .build()

        assertThat(result.task(":compileJava")?.outcome).isEqualTo(NO_SOURCE)
    }

    @Test
    internal fun `should run with correct java`(@TempDir tempDir: Path) {
        """
            org.gradle.java.home=${jdks.getInstallation(VERSION_1_8).toFile().canonicalPath}
            installation.jdk.11=${jdks.getInstallation(VERSION_11).toFile().canonicalPath}
        """.trimIndent()
                .dir(tempDir).name("gradle.properties").write()

        """
            plugins {
                id 'java'
                id 'fr.jcgay.gradle-jdk-chooser-plugin'
            }
            
            java {
               sourceCompatibility = JavaVersion.VERSION_11
               targetCompatibility = JavaVersion.VERSION_11
            }
            
            tasks.create("validateCurrentJdk") {
                if (JavaVersion.current() != JavaVersion.VERSION_1_8) {
                    throw new TaskExecutionException(it, new IllegalStateException("You should run the build with a JDK 8 😇"))
                }
            }
            
            tasks.compileJava.dependsOn("validateCurrentJdk")
            
            tasks.create("runMyClass", JavaExec) {
              classpath = sourceSets.main.runtimeClasspath

              main = 'MyClass'
            }
        """.trimIndent()
                .dir(tempDir).name("build.gradle").write()

        """
            public class MyClass {
                public static void main(String... args) {
                    System.out.println(java.util.List.of("Hello", "World!"));
                }
            }
        """.trimIndent()
                .dir(tempDir).name("MyClass.java").into(MAIN_JAVA).write()

        val result: BuildResult = GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withGradleVersion(gradleVersion)
                .withArguments("runMyClass")
                .withPluginClasspath()
                .build()

        assertThat(result.output).contains("[Hello, World!]")
    }

    @Test
    internal fun `should run test with correct java`(@TempDir tempDir: Path) {
        """
            org.gradle.java.home=${jdks.getInstallation(VERSION_1_8).toFile().canonicalPath}
            installation.jdk.11=${jdks.getInstallation(VERSION_11).toFile().canonicalPath}
        """.trimIndent()
                .dir(tempDir).name("gradle.properties").write()

        """
            plugins {
                id 'java'
                id 'fr.jcgay.gradle-jdk-chooser-plugin'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                testImplementation "junit:junit:4.13"
            }
            
            java {
               sourceCompatibility = JavaVersion.VERSION_11
               targetCompatibility = JavaVersion.VERSION_11
            }
            
            tasks.create("validateCurrentJdk") {
                if (JavaVersion.current() != JavaVersion.VERSION_1_8) {
                    throw new TaskExecutionException(it, new IllegalStateException("You should run the build with a JDK 8 😇"))
                }
            }
            
            tasks.compileJava.dependsOn("validateCurrentJdk")
            
        """.trimIndent()
                .dir(tempDir).name("build.gradle").write()

        """
            import org.junit.*;
            import java.util.*;
            
            public class UnitTest {
            
                @Test
                public void should_success() {
                    Assert.assertEquals(List.of("1"), List.of("1"));
                }
            }
        """.trimIndent()
                .dir(tempDir).name("UnitTest.java").into(UNIT_TEST_JAVA).write()

        val result: BuildResult = GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withGradleVersion(gradleVersion)
                .withArguments("test")
                .withPluginClasspath()
                .build()

        assertThat(result.task(":test")?.outcome).isEqualTo(SUCCESS)
    }

}