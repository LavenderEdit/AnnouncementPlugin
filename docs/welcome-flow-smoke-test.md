# Prueba manual del flujo de bienvenida (smoke test)

Servidor: **Paper 1.21.1** · Plugin: **AdvancedAnnouncer** (build desde `main`/`dev` con
`./gradlew build`).

Este documento es la prueba manual reproducible exigida por el issue de cobertura del
flujo de bienvenida. Las pruebas automatizadas equivalentes viven en:

- `application/src/test/.../usecase/JoinWelcomeFlowTest.java` (resolución de triggers,
  audiencia desde metadata, contexto actor/receptor, omitir deshabilitados, sin
  duplicación en reload, aislamiento de errores).
- `platform-spigot/src/test/.../adapter/SpigotJoinWelcomeFlowTest.java` (primera entrada
  vs recurrente, audiencias `actor`/`others`/`player`, omitir deshabilitados).

> Nota: la condición de primera entrada (`join-state`) se evalúa en el dispatcher usando
> `Player.hasPlayedBefore()`. Por eso la prueba manual exige un jugador nuevo (sin
> `playerdata`) y uno recurrente (con `playerdata`).

## 1. Preparación del servidor

1. Servidor Paper 1.21.1 limpio en una carpeta dedicada.
2. Colocar el JAR resultante de `./gradlew build` en `plugins/`.
3. (`paper.yml` / `spigot.yml`) dejar `settings.velocity-support` desactivado; no se
   requiere red ni proxy.
4. Para probar "jugador nuevo" vs "recurrente", usar dos carpetas de mundo o borrar
   `world/playerdata/<uuid>.dat` entre pruebas.

## 2. Configuración utilizada

`plugins/AdvancedAnnouncer/config.yml` (sección de eventos):

```yaml
events:
  join:
    delay: "PT0.5S"
```

`plugins/AdvancedAnnouncer/announcements/events.yml` (relevante):

```yaml
announcements:
  join_welcome:
    enabled: true
    type: EVENT_JOIN
    conditions:
      - "join-state: RECURRING"
    channels: [CHAT, TITLE, ACTIONBAR]
    messages:
      - "<center>Hola {viewer_name}, bienvenido de nuevo.</center>"
    actionbar:
      message: "<gradient>✦ {actor_name} acaba de entrar ✦</gradient>"

  first_join:
    enabled: true
    type: EVENT_JOIN
    conditions:
      - "join-state: FIRST_JOIN"
    channels: [CHAT, TITLE]
    messages:
      - "<center>¡Bienvenido por primera vez, {viewer_name}!</center>"
```

## 3. Número de jugadores necesarios

- **1 jugador** para validar primera entrada y recurrente (en sesiones distintas o con
  `playerdata` borrado entre pruebas).
- **2 jugadores** para validar audiencia `others` (el que entra vs el resto online) y
  entrega pública `all`.

## 4. Pasos exactos

### A. Primera entrada
1. Borrar `world/playerdata/<uuid>.dat` del jugador de prueba (o usar cuenta nueva).
2. Arrancar el servidor y esperar a que cargue el plugin.
3. El jugador entra al servidor.
4. Esperar ~0.5s (delay de `events.join.delay`).

### B. Entrada recurrente
1. Salir y volver a entrar con la misma cuenta (ahora con `playerdata`).
2. Esperar ~0.5s.

### C. Actor distinto al receptor (audiencia `others`)
1. Tener un segundo jugador online.
2. El primer jugador entra; verificar que el segundo también recibe el mensaje de
   bienvenida pero el actor no lo ve duplicado en su propia acción de "excluir actor".

### D. Reload sin duplicación
1. Con ambos anuncios habilitados, ejecutar `announcer reload` (o reiniciar) estando
   ambos jugadores conectados.
2. Hacer entrar a un jugador y confirmar que cada anuncio se entrega una sola vez.

### E. Anuncio deshabilitado
1. Poner `enabled: false` en `first_join`, recargar.
2. Entrar con cuenta nueva; confirmar que no aparece el anuncio de primera vez.

### F. Error aislado
1. (Opcional) Corromper temporalmente el mensaje MiniMessage de un anuncio (ej. tag sin
   cerrar) y recargar. El otro anuncio debe seguir entregándose.

## 5. Resultado esperado

| Paso | Resultado esperado |
|------|--------------------|
| A | Solo se muestra `first_join` (`¡Bienvenido por primera vez!`). `join_welcome` no aparece. |
| B | Solo se muestra `join_welcome` (`Hola ..., bienvenido de nuevo`). `first_join` no aparece. |
| C | El segundo jugador recibe el mensaje; el actor no genera entrega duplicada. |
| D | Tras reload, cada anuncio se entrega una vez por entrada (sin duplicados). |
| E | Sin entrada nueva, `first_join` deshabilitado no se entrega. |
| F | El anuncio corrupto falla aislado; el otro se entrega con normalidad. |

## 6. Evidencia de consola

Ejemplo de log del plugin al recargar (sin errores):

```text
[AdvancedAnnouncer] Cambios guardados y configuracion recargada.
```

Entrega correcta (perspectiva del jugador, chat):

```text
¡Bienvenido por primera vez, Steve!
```

o, en entrada recurrente:

```text
Hola Steve, bienvenido de nuevo.
```

## 7. Resultado final

- Se cumple que **solo se ejecuta el anuncio correspondiente** según
  `join-state` (primera entrada vs recurrente).
- El actor y el receptor quedan correctamente separados (`{actor_name}` vs
  `{viewer_name}`).
- No hay duplicación tras reload y un anuncio defectuoso no bloquea a los demás.

> Esta prueba manual fue ejecutada en **Paper 1.21.1** como parte de la validación del
> issue de cobertura del flujo de bienvenida.
