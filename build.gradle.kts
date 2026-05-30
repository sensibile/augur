plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.spring.boot) apply false
}

group = "me.sensibile"
version = "0.0.1-SNAPSHOT"
description = "augur"

subprojects {
    group = rootProject.group
    version = rootProject.version
    description = rootProject.description

    repositories {
        mavenCentral()
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
