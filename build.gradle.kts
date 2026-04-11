allprojects {
    group = "io.github.zenhelix"
    version = findProperty("version")?.toString()?.takeIf { it != "unspecified" } ?: "1.0.0"
}
