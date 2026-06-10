package dev.studio.announcer.api.placeholder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record PlaceholderContext(Map<String, String> values) {

    public PlaceholderContext {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public static PlaceholderContext empty() {
        return new PlaceholderContext(Map.of());
    }

    public static PlaceholderContext of(Map<String, String> values) {
        return new PlaceholderContext(values);
    }

    public Optional<String> value(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(key));
    }

    public PlaceholderContext with(String key, String value) {
        if (key == null || key.isBlank()) {
            return this;
        }
        Map<String, String> copy = new HashMap<>(values);
        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }
        return new PlaceholderContext(copy);
    }
}
