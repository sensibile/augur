plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
}

dependencies {
    testImplementation(libs.kotlin.test.junit5)
}
