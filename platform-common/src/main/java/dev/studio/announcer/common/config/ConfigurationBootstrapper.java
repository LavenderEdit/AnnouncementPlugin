package dev.studio.announcer.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class ConfigurationBootstrapper {
    private final ConfigurationPaths paths;

    public ConfigurationBootstrapper(ConfigurationPaths paths) {
        this.paths = Objects.requireNonNull(paths, "paths");
    }

    public void bootstrap() {
        try {
            Files.createDirectories(paths.dataDirectory());
            Files.createDirectories(paths.announcementsDirectory());
            Files.createDirectories(paths.messagesDirectory());
            Files.createDirectories(paths.menusDirectory());
            Files.createDirectories(paths.backupsDirectory());
            createIfMissing(paths.configFile(), defaultConfig());
            createIfMissing(paths.avatarCacheFile(), "avatars: {}\n");
            Map.of(
                    paths.announcementsDirectory().resolve("global.yml"), defaultGlobalAnnouncements(),
                    paths.announcementsDirectory().resolve("vip.yml"), "announcements: {}\n",
                    paths.announcementsDirectory().resolve("events.yml"), defaultEventAnnouncements(),
                    paths.messagesDirectory().resolve("en.yml"), defaultMessages("en"),
                    paths.messagesDirectory().resolve("es.yml"), defaultMessages("es"),
                    paths.menusDirectory().resolve("main.yml"), defaultMainMenu(),
                    paths.menusDirectory().resolve("editor.yml"), defaultEditorMenu()
            ).forEach(this::createIfMissing);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not bootstrap AdvancedAnnouncer configuration.", ex);
        }
    }

    private void createIfMissing(Path file, String content) {
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, content);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create config file: " + file, ex);
        }
    }

    private String defaultConfig() {
        return """
                config-version: 1
                language: en
                debug: false

                server:
                  id: "server-01"
                  groups:
                    - "default"

                storage:
                  type: yaml

                redis:
                  enabled: false
                  uri: "redis://localhost:6379"
                  channel: "advanced_announcer:broadcast"
                  ignore-self: true
                  reconnect-delay-seconds: 5
                  publish-scheduled-network-announcements: false

                discord:
                  enabled: false
                  webhook:
                    enabled: false
                    url: ""
                    username: "AdvancedAnnouncer"
                    color: 16171844
                    footer: ""
                    thumbnail-url: ""
                    timestamp: true
                  discordsrv:
                    enabled: false
                    channel-whitelist: []
                    allowed-role-ids: []
                    cooldown-seconds: 3
                    minecraft-format: "<aqua>%discord_user%</aqua>: <white>%discord_message%</white>"

                placeholderapi:
                  enabled: true

                skinsrestorer:
                  enabled: true

                metrics:
                  enabled: true

                update-checker:
                  enabled: true

                permissions:
                  admin: announcer.admin
                  receive-default: announcer.receive.alert

                folia:
                  compatibility-mode: auto

                scheduler:
                  enabled: true
                  default-interval-seconds: 300

                defaults:
                  enabled: true
                  priority: 0
                  channels:
                    - CHAT
                """;
    }

    private String defaultGlobalAnnouncements() {
        return """
                announcements:
                  welcome:
                    name: Welcome
                    enabled: true
                    type: GLOBAL
                    channels:
                      - CHAT
                    messages:
                      - "<green>Welcome to the server, {player_name}!</green>"
                    priority: 0
                    interval: PT5M
                """;
    }

    private String defaultEventAnnouncements() {
        return """
                announcements:
                  join_welcome:
                    name: Join Welcome
                    enabled: false
                    type: EVENT_JOIN
                    channels:
                      - CHAT
                    messages:
                      - "<gray>Hello <yellow>{player_name}</yellow>!</gray>"
                    priority: 0
                """;
    }

    private String defaultMessages(String language) {
        if ("es".equals(language)) {
            return """
                    reload-success: "<green>Configuracion recargada.</green>"
                    reload-error: "<red>No se pudo recargar la configuracion.</red>"
                    no-permission: "<red>No tienes permiso.</red>"
                    """;
        }
        return """
                reload-success: "<green>Configuration reloaded.</green>"
                reload-error: "<red>Configuration reload failed.</red>"
                no-permission: "<red>You do not have permission.</red>"
                """;
    }

    private String defaultMainMenu() {
        return """
                title: "<gold>AdvancedAnnouncer</gold>"
                size: 27
                items:
                  scheduler:
                    slot: 11
                    material: CLOCK
                    name: "<yellow>Scheduler</yellow>"
                  events:
                    slot: 13
                    material: BELL
                    name: "<aqua>Events</aqua>"
                  styles:
                    slot: 15
                    material: BRUSH
                    name: "<light_purple>Styles</light_purple>"
                  save:
                    slot: 26
                    material: EMERALD
                    name: "<green>Save</green>"
                """;
    }

    private String defaultEditorMenu() {
        return """
                title: "<gold>Announcement Editor</gold>"
                page-size: 45
                """;
    }
}
