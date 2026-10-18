// Gedeelde instellingen voor alle plugins. Een plugin-map heeft alleen een eigen
// build.gradle.kts met version en description nodig.
subprojects {
    apply(plugin = "java")

    group = "nl.codebyvision"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    dependencies {
        "compileOnly"("io.papermc.paper:paper-api:${rootProject.property("paperVersion")}")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    // ${version} en ${description} in plugin.yml worden ingevuld vanuit build.gradle.kts
    tasks.named<ProcessResources>("processResources") {
        val props = mapOf("version" to project.version.toString(), "description" to (project.description ?: ""))
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") { expand(props) }
    }

    // Vaste bestandsnaam zonder versie, zodat een update de oude jar overschrijft
    tasks.named<Jar>("jar") {
        archiveFileName.set("${project.name}.jar")
    }

    // Kopieert de jar naar ../plugins, zodat je lokale server (en na een merge Etheron) hem oppakt
    val deploy = tasks.register<Copy>("deploy") {
        from(tasks.named("jar"))
        into(rootDir.resolve("../plugins"))
    }
    if (project.name != "vision-template") {
        tasks.named("build") { finalizedBy(deploy) }
    }
}
