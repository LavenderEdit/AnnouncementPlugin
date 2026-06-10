package dev.studio.announcer.api.network;

public record NetworkBroadcastRequest(
        String originServer,
        String targetGroup,
        String permission,
        String content,
        String packetType,
        String sound) {

    public NetworkBroadcastRequest {
        originServer = normalize(originServer);
        targetGroup = normalize(targetGroup);
        permission = normalize(permission);
        content = normalize(content);
        packetType = normalize(packetType);
        sound = normalize(sound);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
