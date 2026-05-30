plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

dependencies {
    api(project(":augur-rule-core"))

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test.junit5)
}
