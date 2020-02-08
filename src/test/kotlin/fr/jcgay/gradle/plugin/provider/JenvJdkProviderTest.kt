package fr.jcgay.gradle.plugin.provider

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.api.JavaVersion.VERSION_1_6
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Project
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class JenvJdkProviderTest {

    @RepeatedTest(10)
    internal fun `should return installation managed by jEnv`(@TempDir home: Path) {
        listOf("1.6", "1.6.0.65", "1.8", "1.8.0.232", "11.0", "11.0.5", "11.0.5.hs-adpt", "openjdk64-1.8.0.232", "openjdk64-11.0.5", "oracle64-1.6.0.65")
                .shuffled()
                .forEach { jEnvAdd(it, home) }

        val provider = JenvJdkProvider(home)

        assertThat(provider.findInstallation(VERSION_1_6, anyProject())).isEqualTo(home.resolve("1.6"))
        assertThat(provider.findInstallation(VERSION_1_8, anyProject())).isEqualTo(home.resolve("1.8"))
        assertThat(provider.findInstallation(VERSION_11, anyProject())).isEqualTo(home.resolve("11.0"))
    }

    @Test
    internal fun `should return null when no installation found`(@TempDir home: Path) {
        jEnvAdd("1.6", home)

        val provider = JenvJdkProvider(home)

        assertThat(provider.findInstallation(VERSION_1_8, anyProject())).isNull()
    }

    @Test
    internal fun `should return null when jEnv is not installed`() {
        val provider = JenvJdkProvider(File("does-not-exists").toPath())

        assertThat(provider.findInstallation(VERSION_1_8, anyProject())).isNull()
    }

    private fun jEnvAdd(version: String, home: Path) {
        home.resolve(version).toFile().mkdir()
    }

    private fun anyProject(): Project = mockk()
}