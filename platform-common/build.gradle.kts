description = "Shared infrastructure utilities for AdvancedAnnouncer platforms."

dependencies {
    api(project(":core-api"))
    api(project(":core-domain"))
    api("net.kyori:adventure-api:4.17.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.0")
    implementation("io.lettuce:lettuce-core:7.6.0.RELEASE")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
}
