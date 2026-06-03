plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
}

dependencies {
    api(project(":augur-rule-core"))

    testImplementation(libs.kotlin.test.junit5)
}
