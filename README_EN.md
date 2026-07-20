# AdvancedAnnouncer

Modern announcement engine for Paper/Spigot/Folia with Clean Architecture, Adventure/MiniMessage, in-game GUI, Redis Pub/Sub and Discord integrations.

Spanish documentation: [README.md](README.md)

## Features

- Announcements through `CHAT`, `TITLE`, `SUBTITLE`, `ACTIONBAR`, `BOSSBAR`, `TOAST`, `SOUND` and `DISCORD_WEBHOOK`.
- Adventure/MiniMessage rendering with internal placeholders.
- Optional PlaceholderAPI support.
- Priority-aware BossBars and ActionBars with cleanup on quit/disable.
- Virtual toast notifications backed by advancements.
- In-game editor with Bukkit menus and an anvil-style text input flow.
- Configurate YAML storage: `config.yml`, `announcements/*.yml`, `messages/*.yml` and `menus/*.yml`.
- Safe reload: invalid configuration does not replace the previous valid runtime state.
- Legacy migration from the archived WelcomeDonations project.
- Redis Pub/Sub for cross-server announcements.
- Discord webhook outbound bridge and optional DiscordSRV inbound bridge.
- Bukkit scheduler fallback with a Folia-ready scheduler abstraction.

## Requirements

- Java 21.
- Paper/Spigot 1.21.x. Paper 1.21.1 is recommended.
- The included Gradle Wrapper.
- Optional: PlaceholderAPI, DiscordSRV, Redis, PacketEvents/ProtocolLib depending on the integrations you enable.

## Build

From the project root:

```powershell
.\gradlew.bat test
.\gradlew.bat :platform-spigot:build
```

The plugin artifact is produced at:

```text
platform-spigot/build/libs/AdvancedAnnouncer-0.1.5-SNAPSHOT.jar
```

## Installation

1. Build the project or use the generated `.jar`.
2. Copy `platform-spigot/build/libs/AdvancedAnnouncer-0.1.5-SNAPSHOT.jar` into your server `plugins/` folder.
3. Start the server.
4. Verify that `plugins/AdvancedAnnouncer/` is created.
5. Run `/announcer version` and `/announcer debug`.

## Commands

| Command | Description |
| --- | --- |
| `/announcer` | Shows command help. |
| `/announcer version` | Shows plugin version, platform and integration state. |
| `/announcer debug` | Shows platform, scheduler, Folia, PlaceholderAPI, Redis, Discord and loaded announcement state. |
| `/announcer list` | Lists loaded announcements. |
| `/announcer create <id>` | Creates a basic YAML-backed CHAT announcement. |
| `/announcer delete <id>` | Deletes an announcement. |
| `/announcer toggle <id>` | Enables or disables an announcement. |
| `/announcer send <id>` | Sends an announcement to valid audiences. |
| `/announcer preview <id>` | Sends a preview only to the executing player. |
| `/announcer reload` | Safely reloads config, announcements and scheduler. |
| `/announcer migrate` | Migrates legacy WelcomeDonations formats when available. |
| `/announcer editor` | Opens the in-game editor. |
| `/announcer redis test` | Runs Redis diagnostics. |
| `/announcer discord test` | Sends a Discord bridge test when configured. |

## Permissions

| Permission | Use |
| --- | --- |
| `announcer.admin` | Full access. |
| `announcer.editor` | Opens the GUI editor. |
| `announcer.migrate` | Runs legacy migration. |
| `announcer.reload` | Reloads configuration. |
| `announcer.send` | Manually sends announcements. |
| `announcer.preview` | Previews announcements. |
| `announcer.toggle` | Toggles announcements. |
| `announcer.debug` | Views debug state. |
| `announcer.redis` | Uses Redis diagnostics. |
| `announcer.discord` | Uses Discord diagnostics. |
| `announcer.receive.alert` | Receives general announcements. |
| `announcer.receive.vip` | Receives VIP announcements. |
| `announcer.bypass.cooldown` | Reserved for bypassing cooldowns. |

## Configuration

Main files:

- `plugins/AdvancedAnnouncer/config.yml`: language, server identity, Redis, Discord, scheduler and defaults.
- `plugins/AdvancedAnnouncer/announcements/*.yml`: announcement definitions.
- `plugins/AdvancedAnnouncer/messages/en.yml` and `messages/es.yml`: administration messages.
- `plugins/AdvancedAnnouncer/menus/*.yml`: menu defaults.
- `plugins/AdvancedAnnouncer/backups/`: backups before migrations or destructive saves.

Minimal example:

```yaml
announcements:
  welcome:
    name: Welcome
    enabled: true
    type: GLOBAL
    channels:
      - CHAT
      - ACTIONBAR
    messages:
      - "<green>Welcome, {player_name}!</green>"
    actionbar:
      message: "<yellow>Enjoy your stay.</yellow>"
      duration: PT3S
      priority: 5
    priority: 1
    interval: PT5M
```

Validation covers MiniMessage, cron, permissions, server/group targets, sounds, toast materials, Redis URI and Discord webhook URL.

## Redis

Redis is disabled by default.

```yaml
server:
  id: "survival-01"
  groups:
    - "survival"

redis:
  enabled: true
  uri: "redis://localhost:6379"
  channel: "advanced_announcer:broadcast"
  ignore-self: true
  reconnect-delay-seconds: 5
  publish-scheduled-network-announcements: false
```

Test command:

```text
/announcer redis test
```

For a cross-server smoke test, run two servers with different `server.id` values, configure compatible groups and send a `type: NETWORK` announcement.

## Discord

Discord is disabled by default.

Outbound webhook:

```yaml
discord:
  enabled: true
  webhook:
    enabled: true
    url: "https://discord.com/api/webhooks/..."
    username: "AdvancedAnnouncer"
    color: 16171844
    footer: "play.example.net"
    thumbnail-url: ""
    timestamp: true
```

Test command:

```text
/announcer discord test
```

DiscordSRV inbound:

```yaml
discord:
  enabled: true
  discordsrv:
    enabled: true
    channel-whitelist:
      - "123456789012345678"
    allowed-role-ids: []
    cooldown-seconds: 3
    minecraft-format: "<aqua>%discord_user%</aqua>: <white>%discord_message%</white>"
```

DiscordSRV is a `softdepend`: the plugin still starts when DiscordSRV is not installed.

## Minecraft Smoke Test Guide

1. Run `.\gradlew.bat :platform-spigot:build`.
2. Copy `platform-spigot/build/libs/AdvancedAnnouncer-0.1.5-SNAPSHOT.jar` into `plugins/`.
3. Start Paper 1.21.x.
4. Join as an operator.
5. Run `/announcer version`.
6. Run `/announcer debug`.
7. Run `/announcer list`.
8. Run `/announcer preview welcome`.
9. Run `/announcer send welcome`.
10. Run `/announcer editor` and test create, duplicate, toggle, preview and save.
11. Edit a YAML file under `plugins/AdvancedAnnouncer/announcements/`.
12. Run `/announcer reload`.
13. Test `CHAT`, `TITLE`, `ACTIONBAR`, `BOSSBAR`, `TOAST` and `SOUND`.
14. Optional: enable Redis and run `/announcer redis test`.
15. Optional: enable Discord webhook and run `/announcer discord test`.

## Troubleshooting

- If the plugin does not load, confirm Java 21 and Paper/Spigot 1.21.x.
- If reload fails, read the command errors and console output; the previous runtime state is preserved.
- If MiniMessage fails, check closed tags such as `<green>text</green>`.
- If Redis does not connect, check the URI, firewall and `redis.enabled`.
- If Discord does not send, check `discord.enabled`, `discord.webhook.enabled` and the webhook URL.
- If DiscordSRV does not work, confirm DiscordSRV is installed and the channel/role is allowed.

## Architecture

- `core-api`: public contracts without Bukkit/Paper/Redis/DiscordSRV.
- `core-domain`: pure announcement model and baseline validation.
- `application`: use cases and command orchestration.
- `platform-common`: Configurate, MiniMessage, Redis/Jackson, HTTP webhook, scheduler and neutral utilities.
- `platform-spigot`: Bukkit/Paper bootstrap, commands, GUI, Adventure audiences, sounds and server bridges.
- `legacy/welcomedonations-maven`: archived Maven implementation kept as migration reference.

## Author(s)
Made with passion by two developers who enjoy playing Minecraft a lot.

- Juan S Pimentel Lalangui
- Bryan A Vidal Crispin