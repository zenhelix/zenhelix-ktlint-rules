plugins {
    id("zenhelix.base")
    id("zenhelix.publishing")
}

dependencies {
    compileOnly(libs.ktlint.rule.engine.core)
    compileOnly(libs.ktlint.cli.ruleset.core)

    testImplementation(libs.ktlint.test)
    testImplementation(libs.ktlint.rule.engine.core)
    testRuntimeOnly(libs.slf4j.simple)
}
