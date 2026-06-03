plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kover)
}

dependencies {
    implementation(project(":augur-rule-core"))
    implementation(project(":augur-rule-json"))
    implementation(project(":augur-rule-management"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.kopring.bricks.concurrency.control.starter)
    implementation(libs.kopring.bricks.webmvc.error.starter)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.test.autoconfigure)
}
