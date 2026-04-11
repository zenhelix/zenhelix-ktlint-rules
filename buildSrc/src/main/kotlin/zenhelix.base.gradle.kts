import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    `java-library`
}

val jdkVersion = JavaVersion.VERSION_17

java {
    sourceCompatibility = jdkVersion
    targetCompatibility = jdkVersion
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    explicitApi()

    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jdkVersion.toString()))
        languageVersion.set(KotlinVersion.KOTLIN_2_3)
        apiVersion.set(KotlinVersion.KOTLIN_2_3)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}
