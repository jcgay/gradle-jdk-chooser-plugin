package fr.jcgay.gradle.plugin.provider

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class SdkmanProviderTest {

    @RepeatedTest(5)
    internal fun `should return installation managed by SDKMAN!`(@TempDir home: Path) {
        listOf("11.0.5.hs-adpt", "11.0.6.hs-adpt", "8.0.212.hs-adpt", "current", "8.0.232.hs-adpt")
                .shuffled()
                .forEach { sdkAdd(it, home) }

        val provider = SdkmanProvider(home)

        assertThat(provider.findInstallation(JavaVersion.VERSION_1_8, anyProject())).isEqualTo(home.resolve("8.0.232.hs-adpt"))
        assertThat(provider.findInstallation(JavaVersion.VERSION_11, anyProject())).isEqualTo(home.resolve("11.0.6.hs-adpt"))
    }

    @Test
    internal fun `should return null when no installation found`(@TempDir home: Path) {
        sdkAdd("11.0.6.hs-adpt", home)

        val provider = SdkmanProvider(home)

        assertThat(provider.findInstallation(JavaVersion.VERSION_1_8, anyProject())).isNull()
    }

    @Test
    internal fun `should return null when SDKMAN! is not installed`() {
        val provider = SdkmanProvider(File("does-not-exists").toPath())

        assertThat(provider.findInstallation(JavaVersion.VERSION_1_8, anyProject())).isNull()
    }

    private fun sdkAdd(version: String, home: Path) {
        home.resolve(version).toFile().mkdir()
    }

    private fun anyProject(): Project = mockk()
}