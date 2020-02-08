package fr.jcgay.gradle.plugin

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.nio.file.Path


interface JdkProvider {

    fun findInstallation(version: JavaVersion, project: Project): Path?
}