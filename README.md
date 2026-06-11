# AdvancedAnnouncer

Plugin moderno de anuncios para Paper/Spigot/Folia con Clean Architecture, Adventure/MiniMessage, GUI in-game, Redis Pub/Sub y Discord.

English documentation: [README_EN.md](README_EN.md)

## Caracteristicas

- Anuncios por `CHAT`, `TITLE`, `SUBTITLE`, `ACTIONBAR`, `BOSSBAR`, `TOAST`, `SOUND` y `DISCORD_WEBHOOK`.
- Mensajes con Adventure/MiniMessage y placeholders internos.
- PlaceholderAPI como dependencia opcional.
- BossBars y ActionBars con prioridad y limpieza al salir/desactivar.
- Toasts virtuales basados en advancements.
- Editor in-game con menus Bukkit e input tipo anvil.
- Configuracion YAML con Configurate: `config.yml`, `announcements/*.yml`, `messages/*.yml` y `menus/*.yml`.
- Reload seguro: una configuracion invalida no reemplaza el estado runtime anterior.
- Migracion legacy desde WelcomeDonations archivado.
- Redis Pub/Sub para anuncios cross-server.
- Discord webhook outbound y DiscordSRV inbound opcional.
- Scheduler compatible con Bukkit y preparado para Folia mediante abstraccion.

## Requisitos

- Java 21.
- Paper/Spigot 1.21.x. Recomendado: Paper 1.21.1.
- Gradle Wrapper incluido en el repositorio.
- Opcional: PlaceholderAPI, DiscordSRV, Redis, PacketEvents/ProtocolLib segun las integraciones que uses.

## Compilar

Desde la raiz del proyecto:

```powershell
.\gradlew.bat test
.\gradlew.bat :platform-spigot:build
```

El plugin se genera en:

```text
platform-spigot/build/libs/AdvancedAnnouncer-0.1.0-SNAPSHOT.jar
```

## Instalacion

1. Compila el proyecto o descarga el `.jar` generado.
2. Copia `platform-spigot/build/libs/AdvancedAnnouncer-0.1.0-SNAPSHOT.jar` a la carpeta `plugins/` de tu servidor Paper/Spigot.
3. Inicia el servidor.
4. Verifica que se cree `plugins/AdvancedAnnouncer/`.
5. Ejecuta `/announcer version` y `/announcer debug`.

## Comandos

| Comando | Descripcion |
| --- | --- |
| `/announcer` | Muestra ayuda base. |
| `/announcer version` | Muestra version del plugin, plataforma e integraciones. |
| `/announcer debug` | Muestra estado de plataforma, scheduler, Folia, PlaceholderAPI, Redis, Discord y anuncios cargados. |
| `/announcer list` | Lista anuncios cargados. |
| `/announcer create <id>` | Crea un anuncio CHAT basico en YAML. |
| `/announcer delete <id>` | Elimina un anuncio. |
| `/announcer toggle <id>` | Activa o desactiva un anuncio. |
| `/announcer send <id>` | Envia un anuncio a las audiencias validas. |
| `/announcer preview <id>` | Previsualiza un anuncio solo para el jugador ejecutor. |
| `/announcer reload` | Recarga config, anuncios y scheduler de forma segura. |
| `/announcer migrate` | Migra formatos legacy de WelcomeDonations cuando existan. |
| `/announcer editor` | Abre el editor in-game. |
| `/announcer redis test` | Diagnostica Redis. |
| `/announcer discord test` | Envia una prueba al bridge Discord si esta configurado. |

## Permisos

| Permiso | Uso |
| --- | --- |
| `announcer.admin` | Acceso completo. |
| `announcer.editor` | Abre el editor GUI. |
| `announcer.migrate` | Ejecuta migracion legacy. |
| `announcer.reload` | Recarga configuracion. |
| `announcer.send` | Envia anuncios manualmente. |
| `announcer.preview` | Previsualiza anuncios. |
| `announcer.toggle` | Activa/desactiva anuncios. |
| `announcer.debug` | Consulta estado debug. |
| `announcer.redis` | Usa diagnosticos Redis. |
| `announcer.discord` | Usa diagnosticos Discord. |
| `announcer.receive.alert` | Recibe anuncios generales. |
| `announcer.receive.vip` | Recibe anuncios VIP. |
| `announcer.bypass.cooldown` | Reservado para saltar cooldowns. |

## Configuracion

Archivos principales:

- `plugins/AdvancedAnnouncer/config.yml`: idioma, servidor, Redis, Discord, scheduler y defaults.
- `plugins/AdvancedAnnouncer/announcements/*.yml`: anuncios por archivo.
- `plugins/AdvancedAnnouncer/messages/en.yml` y `messages/es.yml`: mensajes de administracion.
- `plugins/AdvancedAnnouncer/menus/*.yml`: base para menus.
- `plugins/AdvancedAnnouncer/backups/`: backups antes de migraciones o guardados destructivos.

Ejemplo minimo:

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

La validacion revisa MiniMessage, cron, permisos, servidores/grupos, sonidos, materiales de toast, Redis URI y Discord webhook URL.

## Redis

Redis esta desactivado por defecto.

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

Prueba:

```text
/announcer redis test
```

Para smoke test cross-server, levanta dos servidores con distinto `server.id`, configura grupos compatibles y envia un anuncio `type: NETWORK`.

## Discord

Discord esta desactivado por defecto.

Webhook outbound:

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

Prueba:

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

DiscordSRV es `softdepend`: si no esta instalado, el plugin arranca igual.

## Guia Rapida De Prueba En Minecraft

1. Ejecuta `.\gradlew.bat :platform-spigot:build`.
2. Copia `platform-spigot/build/libs/AdvancedAnnouncer-0.1.0-SNAPSHOT.jar` a `plugins/`.
3. Inicia Paper 1.21.x.
4. Entra al servidor como operador.
5. Ejecuta `/announcer version`.
6. Ejecuta `/announcer debug`.
7. Ejecuta `/announcer list`.
8. Ejecuta `/announcer preview welcome`.
9. Ejecuta `/announcer send welcome`.
10. Ejecuta `/announcer editor` y prueba crear, duplicar, alternar, previsualizar y guardar.
11. Edita un YAML en `plugins/AdvancedAnnouncer/announcements/`.
12. Ejecuta `/announcer reload`.
13. Prueba canales `CHAT`, `TITLE`, `ACTIONBAR`, `BOSSBAR`, `TOAST` y `SOUND`.
14. Opcional: activa Redis y prueba `/announcer redis test`.
15. Opcional: activa Discord webhook y prueba `/announcer discord test`.

## Troubleshooting

- Si el plugin no carga, confirma Java 21 y Paper/Spigot 1.21.x.
- Si un reload falla, revisa los errores del comando y la consola; el runtime anterior se conserva.
- Si MiniMessage falla, valida cierres de tags como `<green>texto</green>`.
- Si Redis no conecta, revisa URI, firewall y que `redis.enabled` este en `true`.
- Si Discord no envia, revisa que `discord.enabled`, `discord.webhook.enabled` y la URL del webhook sean validos.
- Si DiscordSRV no funciona, confirma que DiscordSRV este instalado y que el canal/rol este permitido.

## Arquitectura

- `core-api`: contratos publicos sin Bukkit/Paper/Redis/DiscordSRV.
- `core-domain`: modelo puro de anuncios y validacion base.
- `application`: casos de uso y comandos.
- `platform-common`: Configurate, MiniMessage, Redis/Jackson, webhook HTTP, scheduler y utilidades neutrales.
- `platform-spigot`: bootstrap Bukkit/Paper, comandos, GUI, Adventure audiences, sonidos y bridges de servidor.
- `legacy/welcomedonations-maven`: implementacion Maven antigua archivada como referencia de migracion.

## Autor(es)

Desarrollado con pasión por dos desarrolladores que les gusta mucho Minecraft.
- Juan S Pimentel Lalangui
- Bryan A Vidal Crispin