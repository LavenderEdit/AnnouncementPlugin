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
                    migration-success: "<green>Configuracion legacy migrada.</green>"
                    migration-error: "<red>No se pudo migrar la configuracion legacy.</red>"
                    editor-console-only: "<red>El editor solo puede abrirse dentro del juego.</red>"
                    editor-saved: "<green>Cambios guardados y configuracion recargada.</green>"
                    editor-cancelled: "<yellow>Cambios del editor descartados.</yellow>"
                    redis-disabled: "<yellow>Redis esta desactivado o no conectado.</yellow>"
                    redis-connected: "<green>La conexion Redis esta disponible.</green>"
                    discord-disabled: "<yellow>Discord esta desactivado o no configurado.</yellow>"
                    discord-test-sent: "<green>Solicitud de prueba enviada a Discord.</green>"
                    command-version: "<green>Informacion de version de AdvancedAnnouncer mostrada.</green>"
                    command-debug: "<green>Estado debug de AdvancedAnnouncer mostrado.</green>"
                    announcement-created: "<green>Anuncio creado.</green>"
                    announcement-deleted: "<green>Anuncio eliminado.</green>"
                    announcement-toggled: "<green>Anuncio alternado.</green>"
                    announcement-sent: "<green>Anuncio enviado.</green>"
                    announcement-previewed: "<green>Vista previa del anuncio enviada.</green>"
                    announcement-list-empty: "<yellow>No hay anuncios cargados.</yellow>"
                    announcement-not-found: "<red>Anuncio no encontrado.</red>"
                    """;
        }
        return """
                reload-success: "<green>Configuration reloaded.</green>"
                reload-error: "<red>Configuration reload failed.</red>"
                no-permission: "<red>You do not have permission.</red>"
                migration-success: "<green>Legacy configuration migrated.</green>"
                migration-error: "<red>Legacy migration failed.</red>"
                editor-console-only: "<red>The editor can only be opened in-game.</red>"
                editor-saved: "<green>Changes saved and configuration reloaded.</green>"
                editor-cancelled: "<yellow>Editor changes discarded.</yellow>"
                redis-disabled: "<yellow>Redis is disabled or not connected.</yellow>"
                redis-connected: "<green>Redis connection is available.</green>"
                discord-disabled: "<yellow>Discord is disabled or not configured.</yellow>"
                discord-test-sent: "<green>Discord test request sent.</green>"
                command-version: "<green>AdvancedAnnouncer version information displayed.</green>"
                command-debug: "<green>AdvancedAnnouncer debug state displayed.</green>"
                announcement-created: "<green>Announcement created.</green>"
                announcement-deleted: "<green>Announcement deleted.</green>"
                announcement-toggled: "<green>Announcement toggled.</green>"
                announcement-sent: "<green>Announcement sent.</green>"
                announcement-previewed: "<green>Announcement preview sent.</green>"
                announcement-list-empty: "<yellow>No announcements loaded.</yellow>"
                announcement-not-found: "<red>Announcement not found.</red>"
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
