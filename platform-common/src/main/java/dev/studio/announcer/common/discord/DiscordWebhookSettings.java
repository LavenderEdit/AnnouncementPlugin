package dev.studio.announcer.common.discord;

public record DiscordWebhookSettings(
        String webhookUrl,
        String username,
        int color,
        String footer,
        String thumbnailUrl,
        boolean timestamp) {

    public DiscordWebhookSettings {
        webhookUrl = normalize(webhookUrl);
        username = normalize(username);
        color = color <= 0 ? 0xF6C344 : color;
        footer = normalize(footer);
        thumbnailUrl = normalize(thumbnailUrl);
    }

    public boolean enabled() {
        return !webhookUrl.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
