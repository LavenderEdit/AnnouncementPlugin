package dev.studio.announcer.spigot.discord;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record DiscordSrvSettings(
        boolean enabled,
        Set<String> channelWhitelist,
        Set<String> allowedRoleIds,
        Duration cooldown,
        String minecraftFormat) {

    public DiscordSrvSettings {
        channelWhitelist = normalize(channelWhitelist);
        allowedRoleIds = normalize(allowedRoleIds);
        cooldown = cooldown == null || cooldown.isNegative() ? Duration.ZERO : cooldown;
        minecraftFormat = minecraftFormat == null || minecraftFormat.isBlank()
                ? "<aqua>%discord_user%</aqua>: <white>%discord_message%</white>"
                : minecraftFormat;
    }

    private static Set<String> normalize(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
