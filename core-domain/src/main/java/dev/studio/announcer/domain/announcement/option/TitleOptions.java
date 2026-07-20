package dev.studio.announcer.domain.announcement.option;

import java.time.Duration;

public record TitleOptions(
        String title,
        String subtitle,
        Duration fadeIn,
        Duration stay,
        Duration fadeOut) {

    public TitleOptions {
        title = title == null ? "" : title.trim();
        subtitle = subtitle == null ? "" : subtitle.trim();
        if (title.isBlank() && subtitle.isBlank()) {
            throw new IllegalArgumentException("Title options require a title or subtitle.");
        }
        fadeIn = nonNegative(fadeIn, "Title fade-in");
        stay = positive(stay, "Title stay");
        fadeOut = nonNegative(fadeOut, "Title fade-out");
    }

    private static Duration nonNegative(Duration value, String name) {
        if (value == null || value.isNegative()) {
            throw new IllegalArgumentException(name + " cannot be negative.");
        }
        return value;
    }

    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
        return value;
    }
}
