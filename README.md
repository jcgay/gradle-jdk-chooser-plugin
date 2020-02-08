# Gradle JDK chooser plugin

A plugin to select a JDK matching the Java version targeted.

# Usage

It has been a pain to compile a project targeting a Java version older than the JDK running your build tool.  
The safest solution was to use the same JDK version than the targeted runtime version.

You can rely on the `--release` option from `javac` since JDK 9.

This plugin will try to find a JDK matching the `java.targetCompatibility` in different sources and configure tasks in your build that will need it.

    plugins {
      id "fr.jcgay.gradle-jdk-chooser" version "1.0.0"
    }

# JDK Providers

## jEnv

https://github.com/jenv/jenv

## SDKMAN!

https://sdkman.io

## Properties

Define project projerties (in `gradle.properties`):

    installation.jdk.<majorVersion>=/path/to/jdk
	
Example:

    installation.jdk.6=/path/to/jdk
	installation.jdk.8=/path/to/jdk
	installation.jdk.11=/path/to/jdk

# Build

    ./gradlew build
