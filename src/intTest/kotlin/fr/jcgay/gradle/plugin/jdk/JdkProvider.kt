package fr.jcgay.gradle.plugin.jdk

import org.gradle.api.JavaVersion
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.nio.file.Path


class JdkProvider: BeforeAllCallback {

    private lateinit var rootPath: Path
    private lateinit var versions: Array<out JavaVersion>
    private var installations = HashMap<JavaVersion, Path>()

    override fun beforeAll(context: ExtensionContext?) {
        versions.forEach {
            val installedJdk = JdkInstaller().install(it, rootPath)
            installations[it] = installedJdk
        }
    }

    fun withVersion(vararg versions: JavaVersion): JdkProvider {
        this.versions = versions
        return this
    }

    fun atPath(rootPath: Path): JdkProvider {
        this.rootPath = rootPath
        return this
    }

    fun getInstallation(version: JavaVersion): Path {
        return installations[version] ?: throw RuntimeException("Cannot find a JDK with version $version")
    }
}