plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "augur"

include("augur-rule-core")
include("augur-rule-json")
include("augur-rule-sdk")
include("augur-rule-api")
