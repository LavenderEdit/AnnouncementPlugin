group = "dev.studio"
version = "0.1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java-library")

    group = rootProject.group
    version = rootProject.version

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        add("testImplementation", platform("org.junit:junit-bom:5.10.2"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
    }
}
