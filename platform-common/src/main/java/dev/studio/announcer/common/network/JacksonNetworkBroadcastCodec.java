package dev.studio.announcer.common.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.studio.announcer.api.network.NetworkBroadcastCodec;
import dev.studio.announcer.api.network.NetworkBroadcastRequest;
import java.time.Instant;
import java.util.Set;

public final class JacksonNetworkBroadcastCodec implements NetworkBroadcastCodec {
    private final ObjectMapper objectMapper;

    public JacksonNetworkBroadcastCodec() {
        this(new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    JacksonNetworkBroadcastCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String encode(NetworkBroadcastRequest request) {
        try {
            return objectMapper.writeValueAsString(NetworkPayload.from(request));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not encode network broadcast payload.", ex);
        }
    }

    @Override
    public NetworkBroadcastRequest decode(String payload) {
        try {
            NetworkPayload decoded = objectMapper.readValue(payload, NetworkPayload.class);
            return decoded.toRequest();
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not decode network broadcast payload.", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NetworkPayload(
            String messageId,
            String originServer,
            Set<String> targetServers,
            String targetGroup,
            Set<String> targetGroups,
            String permission,
            String content,
            String packetType,
            String sound,
            Instant createdAt) {

        static NetworkPayload from(NetworkBroadcastRequest request) {
            return new NetworkPayload(
                    request.messageId(),
                    request.originServer(),
                    request.targetServers(),
                    request.targetGroup(),
                    request.targetGroups(),
                    request.permission(),
                    request.content(),
                    request.packetType(),
                    request.sound(),
                    request.createdAt());
        }

        NetworkBroadcastRequest toRequest() {
            return new NetworkBroadcastRequest(
                    messageId,
                    originServer,
                    targetServers,
                    targetGroup,
                    targetGroups,
                    permission,
                    content,
                    packetType,
                    sound,
                    createdAt);
        }
    }
}
