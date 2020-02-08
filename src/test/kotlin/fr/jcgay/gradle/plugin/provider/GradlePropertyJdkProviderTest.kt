package fr.jcgay.gradle.plugin.provider

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GradlePropertyJdkProviderTest {

    @Test
    internal fun `should return installation set in project property`(@MockK project: Project) {
        every { project.findProperty("installation.jdk.8") } returns File("/jdk-8").toPath()
        every { project.findProperty("installation.jdk.11") } returns File("/jdk-11").toPath()

        val provider = GradlePropertyJdkProvider()

        assertThat(provider.findInstallation(VERSION_1_8, project)).isEqualTo(File("/jdk-8").toPath())
        assertThat(provider.findInstallation(VERSION_11, project)).isEqualTo(File("/jdk-11").toPath())
    }

    @Test
    internal fun `should return null when property is not set or null`(@MockK project: Project) {
        every { project.findProperty("installation.jdk.8") } returns null

        val provider = GradlePropertyJdkProvider()

        assertThat(provider.findInstallation(VERSION_1_8, project)).isNull()
    }
}