package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class EditorSession {
    private final AnnouncementRepository repository;
    private final Set<AnnouncementId> originalIds;
    private final Map<AnnouncementId, Announcement> drafts = new LinkedHashMap<>();
    private boolean cancelled;

    public EditorSession(AnnouncementRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        repository.findAll().forEach(announcement -> drafts.put(announcement.id(), announcement));
        this.originalIds = drafts.keySet().stream().collect(Collectors.toUnmodifiableSet());
    }

    public void toggle(AnnouncementId id) {
        ensureOpen();
        Announcement current = requireDraft(id);
        drafts.put(id, current.withEnabled(!current.enabled()));
    }

    public void delete(AnnouncementId id) {
        ensureOpen();
        drafts.remove(id);
    }

    public void duplicate(AnnouncementId sourceId, AnnouncementId targetId) {
        ensureOpen();
        Announcement source = requireDraft(sourceId);
        if (drafts.containsKey(targetId)) {
            throw new IllegalArgumentException("Draft announcement already exists: " + targetId.value());
        }
        drafts.put(targetId, copyWithId(source, targetId));
    }

    public void create(Announcement announcement) {
        ensureOpen();
        AnnouncementId id = announcement.id();
        if (drafts.containsKey(id)) {
            throw new IllegalArgumentException("Draft announcement already exists: " + id.value());
        }
        drafts.put(id, announcement);
    }

    public Optional<Announcement> draft(AnnouncementId id) {
        ensureOpen();
        return Optional.ofNullable(drafts.get(id));
    }

    public List<Announcement> drafts() {
        ensureOpen();
        return drafts.values().stream()
                .sorted(Comparator.comparing(announcement -> announcement.id().value()))
                .toList();
    }

    public void save() {
        ensureOpen();
        originalIds.stream()
                .filter(id -> !drafts.containsKey(id))
                .forEach(repository::deleteById);
        drafts.values().forEach(repository::save);
    }

    public void cancel() {
        cancelled = true;
        drafts.clear();
    }

    public boolean cancelled() {
        return cancelled;
    }

    private Announcement requireDraft(AnnouncementId id) {
        Announcement announcement = drafts.get(id);
        if (announcement == null) {
            throw new IllegalArgumentException("Draft announcement not found: " + id.value());
        }
        return announcement;
    }

    private void ensureOpen() {
        if (cancelled) {
            throw new IllegalStateException("Editor session is already cancelled.");
        }
    }

    private Announcement copyWithId(Announcement source, AnnouncementId targetId) {
        Instant now = Instant.now();
        return new Announcement(
                targetId,
                source.name() + " Copy",
                source.enabled(),
                source.type(),
                source.channels(),
                source.messages(),
                source.permission(),
                source.targetServers(),
                source.targetGroups(),
                source.interval(),
                source.cronExpression(),
                source.priority(),
                source.soundOptions(),
                source.toastOptions(),
                source.bossBarOptions(),
                source.actionBarOptions(),
                source.titleOptions(),
                source.conditions(),
                source.metadata(),
                now,
                now);
    }
}
