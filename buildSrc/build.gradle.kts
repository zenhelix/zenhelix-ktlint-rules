plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugins.kotlin.jvm.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.maven.central.publish)
}
