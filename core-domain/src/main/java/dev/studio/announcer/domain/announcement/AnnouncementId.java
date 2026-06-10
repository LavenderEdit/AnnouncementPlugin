package dev.studio.announcer.domain.announcement;

import java.util.Objects;
import java.util.regex.Pattern;

public record AnnouncementId(String value) {
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_.:-]+");

    public AnnouncementId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Announcement id cannot be blank.");
        }
        if (!VALID_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("Announcement id may only contain letters, numbers, _, ., :, or -.");
        }
    }

    public static AnnouncementId of(String value) {
        return new AnnouncementId(value);
    }
}
