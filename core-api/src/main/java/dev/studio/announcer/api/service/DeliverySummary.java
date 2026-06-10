package dev.studio.announcer.api.service;

public record DeliverySummary(int delivered, int skippedByPermission, int invalid) {

    public DeliverySummary {
        if (delivered < 0 || skippedByPermission < 0 || invalid < 0) {
            throw new IllegalArgumentException("Delivery counters cannot be negative.");
        }
    }

    public static DeliverySummary empty() {
        return new DeliverySummary(0, 0, 0);
    }

    public DeliverySummary plus(DeliverySummary other) {
        if (other == null) {
            return this;
        }
        return new DeliverySummary(
                delivered + other.delivered(),
                skippedByPermission + other.skippedByPermission(),
                invalid + other.invalid());
    }
}
