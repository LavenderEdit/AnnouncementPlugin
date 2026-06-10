description = "Shared infrastructure utilities for AdvancedAnnouncer platforms."

dependencies {
    api(project(":core-api"))
    api(project(":core-domain"))
    api("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
}
