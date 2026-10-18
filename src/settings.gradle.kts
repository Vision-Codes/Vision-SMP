plugins {
    // Downloadt automatisch Java 25 als je die nog niet hebt
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "vision-plugins"

// Elke map met een build.gradle.kts is een plugin; nieuwe mappen worden vanzelf meegenomen
rootDir.listFiles()!!
    .filter { it.resolve("build.gradle.kts").isFile }
    .forEach { include(it.name) }
