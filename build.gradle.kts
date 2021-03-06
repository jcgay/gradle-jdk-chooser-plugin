
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
   kotlin("jvm") version "1.3.61"
   `java-gradle-plugin`
   `maven-publish`
   id("pl.allegro.tech.build.axion-release") version "1.11.0"
   id("com.gradle.plugin-publish") version "0.10.1"
}

scmVersion {
   with(tag) {
      prefix = "v"
      versionSeparator = ""
   }
}
project.version = scmVersion.version

repositories {
	mavenCentral()
}

val intTest = sourceSets.create("intTest") {
   compileClasspath += sourceSets.main.get().output
   runtimeClasspath += sourceSets.main.get().output
}

val intTestImplementation by configurations.getting {
   extendsFrom(configurations.testImplementation.get())
}
configurations["intTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
   implementation(gradleApi())
   implementation(kotlin("stdlib-jdk8"))
   
   testImplementation(platform("org.junit:junit-bom:5.6.0"))
   testImplementation("org.junit.jupiter:junit-jupiter-api")
   testImplementation("org.junit.jupiter:junit-jupiter-params")
   testImplementation("org.assertj:assertj-core:3.14.0")
   testImplementation("io.mockk:mockk:1.9.3")
   testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

   intTestImplementation("com.squareup.okhttp3:okhttp:4.3.1")
   intTestImplementation("org.apache.commons:commons-lang3:3.9")
   intTestImplementation("org.rauschig:jarchivelib:1.0.0")
}

java {
   sourceCompatibility = JavaVersion.VERSION_1_8
   targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
   plugins {
      create("gradleJdkChooserPlugin") {
         id = "fr.jcgay.gradle-jdk-chooser"
         displayName = "JDK Chooser Plugin"
         implementationClass = "fr.jcgay.gradle.plugin.JdkChooserPlugin"
      }
   }
}

pluginBundle {
   website = "https://github.com/jcgay/gradle-jdk-chooser-plugin"
   vcsUrl = "https://github.com/jcgay/gradle-jdk-chooser-plugin"
   description = "A plugin that choose a JDK and configure 'release' javac argument to build your project"
   tags = listOf("javac --release", "jEnv", "SDKMAN!")
}

tasks {
   withType(Test::class) {
      useJUnitPlatform()

      testLogging {
         info.events = setOf(PASSED, FAILED, SKIPPED, STANDARD_OUT, STANDARD_ERROR)
         info.exceptionFormat = TestExceptionFormat.FULL
      }
   }

   withType(KotlinCompile::class) {
      kotlinOptions {
         jvmTarget = JavaVersion.VERSION_1_8.toString()
      }
   }

   val integrationTest = create<Test>("integrationTest") {
      description = "Runs integration tests."
      group = "verification"

      testClassesDirs = intTest.output.classesDirs
      classpath = intTest.runtimeClasspath
      shouldRunAfter("test")
   }

   check {
      dependsOn(integrationTest)
   }

   publish {
      dependsOn(publishPlugins)
   }
}

publishing {
   publications {
      create<MavenPublication>("maven") {
         group = "fr.jcgay.gradle-jdk-chooser-plugin"
         artifactId = "fr.jcgay.gradle-jdk-chooser.gradle.plugin"
         from(components["java"])
      }
   }
}
