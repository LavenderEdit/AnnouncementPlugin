package dev.studio.announcer.api.placeholder;

public interface PlaceholderResolver {

    String resolve(String input, PlaceholderContext context);
}
