plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

dependencies {
    api(project(":augur-rule-management"))
    api(libs.kopring.bricks.event.sourcing.starter)

    implementation(project(":augur-rule-json"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test.junit5)
}
