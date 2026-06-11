package dev.studio.announcer.spigot.discord;

import dev.studio.announcer.api.discord.DiscordInboundMessage;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class DiscordSrvEventForwarder implements AutoCloseable {
    private final DiscordSrvBridgeService bridgeService;
    private boolean registered;

    public DiscordSrvEventForwarder(DiscordSrvBridgeService bridgeService) {
        this.bridgeService = Objects.requireNonNull(bridgeService, "bridgeService");
    }

    public void register() {
        if (!registered) {
            DiscordSRV.api.subscribe(this);
            registered = true;
        }
    }

    @Subscribe
    public void onDiscordGuildMessageReceived(DiscordGuildMessageReceivedEvent event) {
        if (bool(call(call(event, "getAuthor"), "isBot"))) {
            return;
        }
        String content = firstText(call(event, "getMessage"), "getContentRaw", "getContentDisplay", "getContentStripped");
        if (content.isBlank()) {
            return;
        }
        bridgeService.acceptInbound(new DiscordInboundMessage(
                text(call(event, "getChannel"), "getId"),
                text(call(event, "getAuthor"), "getId"),
                firstText(call(event, "getAuthor"), "getEffectiveName", "getName"),
                content,
                roles(call(event, "getMember")),
                Instant.now()));
    }

    @Override
    public void close() {
        if (registered) {
            DiscordSRV.api.unsubscribe(this);
            registered = false;
        }
    }

    private Object call(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private String text(Object target, String methodName) {
        Object value = call(target, methodName);
        return value == null ? "" : value.toString();
    }

    private String firstText(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            String value = text(target, methodName);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean bool(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private Set<String> roles(Object member) {
        Object roles = call(member, "getRoles");
        if (!(roles instanceof Collection<?> collection)) {
            return Set.of();
        }
        return collection.stream()
                .map(role -> text(role, "getId"))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
