import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
   kotlin("jvm") version "1.3.61"
   `java-gradle-plugin`
   `maven-publish`
}

repositories {
	mavenCentral()
}

dependencies {
   implementation(gradleApi())
   implementation(kotlin("stdlib-jdk8"))
   
   testImplementation(platform("org.junit:junit-bom:5.6.0"))
   testImplementation("org.junit.jupiter:junit-jupiter-api")
   testImplementation("org.junit.jupiter:junit-jupiter-params")
   testImplementation("org.assertj:assertj-core:3.14.0")
   testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
   sourceCompatibility = JavaVersion.VERSION_1_8
   targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
   plugins {
      create("gradleJdkChooserPlugin") {
         id = "fr.jcgay.gradle-jdk-chooser-plugin"
         displayName = "JDK Chooser Plugin"
         implementationClass = "fr.jcgay.gradle.plugin.JdkChooserPlugin"
      }
   }
}

val intTest = sourceSets.create("intTest") {
   compileClasspath += sourceSets.main.get().output
   runtimeClasspath += sourceSets.main.get().output
}

configurations["intTestImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["intTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

tasks {
   test {
      useJUnitPlatform()
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
}

publishing {
   publications {
      create<MavenPublication>("maven") {
         group = "fr.jcgay.gradle-jdk-chooser-plugin"
         artifactId = "fr.jcgay.gradle-jdk-chooser-plugin.gradle.plugin"
         version = "1.0-SNAPSHOT"
         from(components["java"])
      }
   }
}
