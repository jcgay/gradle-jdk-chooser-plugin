package fr.jcgay.gradle.plugin

import fr.jcgay.gradle.plugin.dsl.SourceCategory.MAIN_JAVA
import fr.jcgay.gradle.plugin.dsl.SourceCategory.UNIT_TEST_JAVA
import fr.jcgay.gradle.plugin.dsl.dir
import fr.jcgay.gradle.plugin.jdk.JdkProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.gradle.api.JavaVersion
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
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
    val testingJdks = JdkProvider()
            .withVersion(VERSION_1_8, VERSION_11)
            .atPath(File("build").toPath())

    private val gradleVersion = "6.1.1"

    @Test
    internal fun `should use a different jdk when current runtime is not the one targeted`(@TempDir tempDir: Path) {
        """
            org.gradle.java.home=${testingJdks.getInstallation(VERSION_1_8).toFile().canonicalPath}
            installation.jdk.11=${testingJdks.getInstallation(VERSION_11).toFile().canonicalPath}
        """.trimIndent()
                .dir(tempDir).name("gradle.properties").write()

        """
            ${applyPlugins()}
            
            java {
               sourceCompatibility = JavaVersion.VERSION_11
               targetCompatibility = JavaVersion.VERSION_11
            }
            
            jdk.provider = 'GradlePropertyJdkProvider'
            
            ${ensureCurrentJavaVersionIs(VERSION_1_8)}
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
            org.gradle.java.home=${testingJdks.getInstallation(VERSION_1_8).toFile().canonicalPath}
        """.trimIndent()
                .dir(tempDir).name("gradle.properties").write()

        """
            ${applyPlugins()}
            
            java {
               sourceCompatibility = JavaVersion.VERSION_1_8
               targetCompatibility = JavaVersion.VERSION_1_8
            }
            
            jdk.provider = 'GradlePropertyJdkProvider'
            
            ${ensureCurrentJavaVersionIs(VERSION_1_8)}
            
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
            org.gradle.java.home=${testingJdks.getInstallation(VERSION_1_8).toFile().canonicalPath}
            installation.jdk.11=${testingJdks.getInstallation(VERSION_11).toFile().canonicalPath}
        """.trimIndent()
                .dir(tempDir).name("gradle.properties").write()

        """
            ${applyPlugins()}
            
            java {
               sourceCompatibility = JavaVersion.VERSION_11
               targetCompatibility = JavaVersion.VERSION_11
            }
            
            jdk.provider = 'GradlePropertyJdkProvider'
            
            ${ensureCurrentJavaVersionIs(VERSION_1_8)}
            
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
            org.gradle.java.home=${testingJdks.getInstallation(VERSION_1_8).toFile().canonicalPath}
            installation.jdk.11=${testingJdks.getInstallation(VERSION_11).toFile().canonicalPath}
        """.trimIndent()
                .dir(tempDir).name("gradle.properties").write()

        """
            ${applyPlugins()}
            
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
            
            jdk.provider = 'GradlePropertyJdkProvider'
            
            ${ensureCurrentJavaVersionIs(VERSION_1_8)}
            
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

    @Test
    internal fun `should use release javac argument when current runtime is not the one targeted`(@TempDir tempDir: Path) {
        """
            org.gradle.java.home=${testingJdks.getInstallation(VERSION_11).toFile().canonicalPath}
        """.trimIndent()
                .dir(tempDir).name("gradle.properties").write()

        """
            ${applyPlugins()}
            
            java {
               sourceCompatibility = JavaVersion.VERSION_1_8
               targetCompatibility = JavaVersion.VERSION_1_8
            }
            
            jdk.provider = 'GradlePropertyJdkProvider'
            
            ${ensureCurrentJavaVersionIs(VERSION_11)}
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

        val gradle = GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withGradleVersion(gradleVersion)
                .withArguments("compileJava")
                .withPluginClasspath()

        if (JavaVersion.current() < VERSION_11) {
            // I don't know why the GradleRunner is failing 🤷‍♂️
            // It fails when the build is run by a JDK 8 for example
            assertThatIllegalStateException().isThrownBy { gradle.buildAndFail() }
                    .withMessageContaining("Task :compileJava FAILED")
                    .withMessageContaining("""
                    src/main/java/MyClass.java:3: error: cannot find symbol
                            System.out.println(java.util.List.of("Hello", "World!"));
                                                             ^
                      symbol:   method of(String,String)
                      location: interface List
                    1 error
                    """.trimIndent())
        } else {
            val result = gradle.buildAndFail()
            assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(result.output).contains("""
                    src/main/java/MyClass.java:3: error: cannot find symbol
                            System.out.println(java.util.List.of("Hello", "World!"));
                                                             ^
                      symbol:   method of(String,String)
                      location: interface List
                    1 error
                    """.trimIndent())
        }
    }

    private fun applyPlugins(): String {
        return """
                plugins {
                    id 'java'
                    id 'fr.jcgay.gradle-jdk-chooser'
                }
                """
    }

    private fun ensureCurrentJavaVersionIs(version: JavaVersion): String {
        return """
                tasks.create("validateCurrentJdk") {
                    if (JavaVersion.current() != JavaVersion.${version.name}) {
                        throw new TaskExecutionException(it, new IllegalStateException("You should run the build with a JDK ${version.majorVersion} 😇"))
                    }
                }
                
                tasks.compileJava.dependsOn("validateCurrentJdk")
                """
    }

}