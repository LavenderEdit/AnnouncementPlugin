description = "Paper/Spigot bootstrap and adapters for AdvancedAnnouncer."

dependencies {
    implementation(project(":application"))
    implementation(project(":platform-common"))
    implementation(project(":core-api"))
    implementation(project(":core-domain"))

    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("net.kyori:adventure-platform-bukkit:4.3.4")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.17.0")
    compileOnly("xyz.xenondevs.invui:invui:1.49")
    compileOnly("io.github.rysefoxx.anvilgui:anvilgui:1.6.5.4")

    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("AdvancedAnnouncer")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get()
            .filter { it.exists() }
            .map { if (it.isDirectory) it else zipTree(it) }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
