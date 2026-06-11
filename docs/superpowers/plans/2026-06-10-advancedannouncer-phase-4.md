# AdvancedAnnouncer Phase 4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Phase 4 by adding persistent YAML configuration, reload/migration/validation, announcement scheduling, and the in-game editor foundation.

**Architecture:** Configuration and serialization live in `platform-common` because they are platform-neutral. Bukkit/Paper-only GUI and lifecycle wiring live in `platform-spigot`. The application layer receives small ports for reload and editor opening, without depending on Configurate, Bukkit, InvUI, AnvilGUI, or Paper.

**Tech Stack:** Java 21, Gradle Kotlin DSL, Configurate YAML `org.spongepowered:configurate-yaml:4.2.0`, existing Adventure/MiniMessage, Bukkit/Paper API, JUnit 5. InvUI and AnvilGUI coordinates must be verified from their official distribution source at the start of Phase 4D before adding dependencies.

---

## Phase 4A: YAML Configuration And Storage

**Objective:** Replace in-memory-only announcements with Configurate-backed YAML files.

**Files To Create**
- `platform-common/src/main/java/dev/studio/announcer/common/config/AdvancedAnnouncerConfig.java`
- `platform-common/src/main/java/dev/studio/announcer/common/config/ConfigurationPaths.java`
- `platform-common/src/main/java/dev/studio/announcer/common/config/ConfigurationBootstrapper.java`
- `platform-common/src/main/java/dev/studio/announcer/common/config/AnnouncementYamlMapper.java`
- `platform-common/src/main/java/dev/studio/announcer/common/config/YamlAnnouncementRepository.java`
- `platform-common/src/main/java/dev/studio/announcer/common/config/ConfigBackupService.java`
- `platform-common/src/test/java/dev/studio/announcer/common/config/AnnouncementYamlMapperTest.java`
- `platform-common/src/test/java/dev/studio/announcer/common/config/YamlAnnouncementRepositoryTest.java`
- `platform-common/src/test/java/dev/studio/announcer/common/config/ConfigurationBootstrapperTest.java`
- Resource templates under `platform-spigot/src/main/resources/announcements/`, `messages/`, and `menus/`.

**Files To Modify**
- `platform-common/build.gradle.kts`: add `implementation("org.spongepowered:configurate-yaml:4.2.0")`.
- `platform-spigot/src/main/resources/config.yml`: add `config-version`, storage defaults, scheduler defaults, and global permissions.
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/bootstrap/AdvancedAnnouncerPlugin.java`: create config directories, instantiate `YamlAnnouncementRepository`, and load announcements on enable.

**Done When**
- `announcements/global.yml`, `announcements/vip.yml`, and `announcements/events.yml` are copied/created on first boot.
- Announcements load from YAML into `Announcement`.
- Command-created/deleted/toggled announcements persist to disk.
- `InMemoryAnnouncementRepository` remains only for tests/fallback.

**Tests**
- Mapper round-trips chat/title/actionbar/bossbar/toast/sound options.
- Repository saves, lists, finds, deletes, and reloads YAML files.
- Bootstrapper creates missing directories/files without overwriting existing files.
- Backup service creates timestamped backups before destructive saves.

## Phase 4B: Reload, Validation, Messages, And Migration

**Objective:** Make `/announcer reload` real and add safe config validation/migration.

**Files To Create**
- `core-api/src/main/java/dev/studio/announcer/api/service/ConfigurationReloadService.java`
- `application/src/main/java/dev/studio/announcer/application/usecase/ReloadConfigurationUseCase.java`
- `platform-common/src/main/java/dev/studio/announcer/common/config/ConfigurationReloadResult.java`
- `platform-common/src/main/java/dev/studio/announcer/common/config/ConfigurationValidationService.java`
- `platform-common/src/main/java/dev/studio/announcer/common/config/LegacyWelcomeDonationsMigrator.java`
- `platform-common/src/main/java/dev/studio/announcer/common/message/PluginMessages.java`
- `platform-common/src/test/java/dev/studio/announcer/common/config/ConfigurationValidationServiceTest.java`
- `platform-common/src/test/java/dev/studio/announcer/common/config/LegacyWelcomeDonationsMigratorTest.java`
- `application/src/test/java/dev/studio/announcer/application/usecase/ReloadConfigurationUseCaseTest.java`

**Files To Modify**
- `application/src/main/java/dev/studio/announcer/application/command/AnnouncerCommandService.java`: delegate `reload` to `ReloadConfigurationUseCase`.
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/bootstrap/AdvancedAnnouncerPlugin.java`: wire reload service.
- `platform-spigot/src/main/resources/plugin.yml`: add `/announcer migrate` permission and tab support if needed.
- `platform-spigot/src/main/resources/messages/es.yml` and `messages/en.yml`: command/reload/migration messages.

**Done When**
- `/announcer reload` reloads `config.yml`, all announcement YAML files, messages, and menus.
- Invalid configs return structured errors and keep the previous valid runtime state.
- Legacy `legacy/welcomedonations-maven/src/main/resources/config.yml` can migrate welcome/donation/rank formats into new announcement YAML files.
- A backup is created before migration or overwrite.

**Tests**
- Reload succeeds with valid files.
- Reload fails clearly with malformed YAML or invalid MiniMessage.
- Failed reload does not clear existing repository state.
- Legacy migrator converts `welcome.first-join`, `welcome.rejoin`, `donations.format`, and `ranks.format` into announcements.

## Phase 4C: Announcement Scheduler

**Objective:** Schedule interval-based announcements using existing `SchedulerPort`, with reload-safe cleanup.

**Files To Create**
- `core-api/src/main/java/dev/studio/announcer/api/service/AnnouncementSchedulerService.java`
- `application/src/main/java/dev/studio/announcer/application/usecase/StartSchedulersUseCase.java`
- `platform-common/src/main/java/dev/studio/announcer/common/scheduler/DefaultAnnouncementSchedulerService.java`
- `platform-common/src/main/java/dev/studio/announcer/common/scheduler/SchedulerRegistration.java`
- `platform-common/src/test/java/dev/studio/announcer/common/scheduler/DefaultAnnouncementSchedulerServiceTest.java`

**Files To Modify**
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/bootstrap/AdvancedAnnouncerPlugin.java`: start scheduler after config load, restart on reload, stop on disable.
- `platform-spigot/src/main/resources/config.yml`: scheduler defaults and disabled/enabled flag.
- `platform-common/src/main/java/dev/studio/announcer/common/config/AnnouncementYamlMapper.java`: ensure interval/cron values are parsed and validated.

**Done When**
- Enabled announcements with `interval` are scheduled.
- Disabled announcements are ignored.
- Reload cancels old scheduler tasks and starts the new set.
- Cron expressions are validated and stored; execution can be limited to interval scheduling in 4C unless cron support is implemented with a dedicated dependency.

**Tests**
- Schedules interval announcements and calls dispatcher.
- Ignores disabled announcements and announcements without interval.
- Cancels all tasks on reload/stop.
- Rejects invalid interval and malformed cron expression with clear validation errors.

## Phase 4D: In-Game Editor Foundation

**Objective:** Add `/announcer editor` and a usable GUI foundation for managing announcements.

**Dependency Gate**
- Verify InvUI official repository and coordinates before adding Gradle dependency.
- Verify AnvilGUI official or maintained coordinates before adding Gradle dependency.
- If either dependency cannot be resolved reliably, stop and report the exact blocker before implementing GUI code.

**Files To Create**
- `core-api/src/main/java/dev/studio/announcer/api/service/AnnouncementEditorService.java`
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/gui/SpigotAnnouncementEditorService.java`
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/gui/EditorSession.java`
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/gui/MainEditorMenu.java`
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/gui/AnnouncementListMenu.java`
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/gui/AnnouncementDetailMenu.java`
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/gui/ConfirmDeleteMenu.java`
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/gui/AnvilTextInputService.java`
- `platform-spigot/src/test/java/dev/studio/announcer/spigot/gui/EditorSessionTest.java`

**Files To Modify**
- `application/src/main/java/dev/studio/announcer/application/command/AnnouncerCommandService.java`: add `editor` command result type or platform hook.
- `platform-spigot/src/main/java/dev/studio/announcer/spigot/command/AnnouncerCommand.java`: open editor only for players with `announcer.editor`.
- `platform-spigot/src/main/resources/plugin.yml`: expose `editor` in usage/permissions.
- `platform-spigot/src/main/resources/menus/main.yml` and `menus/editor.yml`: menu layout defaults.

**Done When**
- `/announcer editor` opens a main menu for players and rejects console.
- Main menu includes Programador, Eventos, Estilos, Guardar.
- Announcement list supports pagination, back, create, edit, duplicate, toggle, delete with confirmation, preview, and save.
- Anvil input validates MiniMessage before applying message edits.
- Save persists to YAML and reloads runtime state.

**Tests**
- Command permission and console rejection tests.
- Editor session mutates draft announcements without saving until Save.
- Cancel discards draft changes.
- Save writes YAML and reloads repository.
- MiniMessage validation rejects malformed input in text edit flow.

## Final Phase 4 Verification

- [x] Run `.\gradlew.bat test`.
- [x] Run `.\gradlew.bat :platform-spigot:build`.
- [x] Run architecture sweep:

```powershell
rg -n "org\.bukkit|io\.papermc|net\.kyori|Bukkit|Player|JavaPlugin|InvUI|AnvilGUI|Configurate" core-api core-domain application
```

- [x] Confirm only `platform-spigot` imports Bukkit/Paper/GUI dependencies.
- [x] Confirm `platform-common` may use Configurate and Adventure only where already intended.
- [x] Confirm final plugin jar exists under `platform-spigot/build/libs/`.
