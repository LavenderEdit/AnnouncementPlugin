package dev.studio.announcer.api.network;

public interface NetworkBroadcastCodec {

    String encode(NetworkBroadcastRequest request);

    NetworkBroadcastRequest decode(String payload);
}
