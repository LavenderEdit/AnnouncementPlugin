# AdvancedAnnouncer

AdvancedAnnouncer is a modern Minecraft Java announcement plugin built as a Gradle multi-module project.

## Project layout

- `core-api`: public ports and contracts.
- `core-domain`: pure announcement domain model and validation.
- `application`: use cases and command orchestration.
- `platform-common`: shared infrastructure utilities.
- `platform-spigot`: Paper/Spigot bootstrap and adapters.
- `legacy/welcomedonations-maven`: archived Maven implementation kept only as migration reference.

## Build

Use the Gradle Wrapper from the project root:

```powershell
.\gradlew.bat test
.\gradlew.bat :platform-spigot:build
```

The plugin artifact is produced by the `platform-spigot` module.
