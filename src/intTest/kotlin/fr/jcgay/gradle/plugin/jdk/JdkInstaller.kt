package fr.jcgay.gradle.plugin.jdk

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.JavaVersion
import org.gradle.api.logging.Logging
import org.rauschig.jarchivelib.ArchiverFactory
import java.nio.file.Files
import java.nio.file.Path


class JdkInstaller {

    private val logger = Logging.getLogger(JdkInstaller::class.java)
    private val client = OkHttpClient.Builder().build()

    fun install(version: JavaVersion, path: Path): Path {
        val destination = path.resolve("jdk-${version.majorVersion}")
        if (Files.exists(destination)) {
            val existingJdk = findJdk(destination)
            logger.lifecycle("JDK {} is available at {}", version.majorVersion, existingJdk.toFile().canonicalPath)
            return existingJdk
        }

        val request = Request.Builder().get()
                .url("https://api.adoptopenjdk.net/v3/binary/latest/${version.majorVersion}/ga/${currentOs()}/${currentArchitecture()}/jdk/hotspot/normal/adoptopenjdk?project=jdk")
                .build()

        val archiveFolder = Files.createTempDirectory("jdk-${version.majorVersion}")
        archiveFolder.toFile().deleteOnExit()

        val archive = download(request, archiveFolder).toFile()
        val archiver = ArchiverFactory.createArchiver(archive)
        archiver.extract(archive, destination.toFile())

        val jdkPath = findJdk(destination)
        logger.lifecycle("JDK {} has been extracted to {}", version.majorVersion, jdkPath.toFile().canonicalPath)
        return jdkPath
    }

    private fun findJdk(destination: Path): Path {
        return Files.list(destination)
                .filter { it.toFile().isDirectory }
                .map {
                    when {
                        SystemUtils.IS_OS_MAC -> it.resolve("Contents/Home")
                        else -> it
                    }
                }
                .findFirst()
                .orElseThrow { RuntimeException("Unable to find a valid JDK in ${destination.toFile().canonicalPath}") }
    }

    private fun download(request: Request, archiveFolder: Path): Path {
        logger.lifecycle("Downloading JDK archive in {} from {}", archiveFolder, request.url)
        client.newCall(request).execute().use { response ->
            val archivePath = archiveFolder.resolve(getFilename(response))
            response.body?.byteStream()?.copyTo(archivePath.toFile().outputStream())
            return archivePath
        }
    }

    private fun getFilename(response: Response): String {
        // Content-Disposition: attachment; filename=OpenJDK8U-jdk_x64_windows_hotspot_8u242b08.zip
        val content = response.headers["Content-Disposition"]
        return content?.substringAfter("filename=") ?: "a-jdk"
    }

    private fun currentArchitecture(): String {
        return when (SystemUtils.OS_ARCH) {
            "x86_64" -> "x64"
            "amd64" -> "x64"
            else -> "x32"
        }
    }

    private fun currentOs(): String {
        return when {
            SystemUtils.IS_OS_WINDOWS -> "windows"
            SystemUtils.IS_OS_MAC -> "mac"
            SystemUtils.IS_OS_LINUX -> "linux"
            SystemUtils.IS_OS_AIX -> "aix"
            SystemUtils.IS_OS_SOLARIS -> "solaris"
            else -> throw RuntimeException("Unable to determine a compatible OS to download AdoptOpenJDK")
        }
    }
}
