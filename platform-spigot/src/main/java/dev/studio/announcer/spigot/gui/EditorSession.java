package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
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

    public void rename(AnnouncementId id, String newName) {
        ensureOpen();
        Announcement current = requireDraft(id);
        Instant now = Instant.now();
        Announcement updated = new Announcement(
                current.id(),
                newName,
                current.enabled(),
                current.type(),
                current.channels(),
                current.messages(),
                current.permission(),
                current.targetServers(),
                current.targetGroups(),
                current.interval(),
                current.cronExpression(),
                current.priority(),
                current.soundOptions(),
                current.toastOptions(),
                current.bossBarOptions(),
                current.actionBarOptions(),
                current.titleOptions(),
                current.conditions(),
                current.metadata(),
                current.createdAt(),
                now);
        drafts.put(id, updated);
    }

    public void toggleChannel(AnnouncementId id, AnnouncementChannel channel) {
        ensureOpen();
        Announcement current = requireDraft(id);
        java.util.EnumSet<AnnouncementChannel> channels = java.util.EnumSet.copyOf(current.channels());
        if (channels.contains(channel)) {
            if (channels.size() > 1) {
                channels.remove(channel);
            }
        } else {
            channels.add(channel);
        }
        Instant now = Instant.now();
        Announcement updated = new Announcement(
                current.id(),
                current.name(),
                current.enabled(),
                current.type(),
                channels,
                current.messages(),
                current.permission(),
                current.targetServers(),
                current.targetGroups(),
                current.interval(),
                current.cronExpression(),
                current.priority(),
                current.soundOptions(),
                current.toastOptions(),
                current.bossBarOptions(),
                current.actionBarOptions(),
                current.titleOptions(),
                current.conditions(),
                current.metadata(),
                current.createdAt(),
                now);
        drafts.put(id, updated);
    }

    public void updateMessage(AnnouncementId id, int index, String newMessage) {
        ensureOpen();
        Announcement current = requireDraft(id);
        List<String> updated = new java.util.ArrayList<>(current.messages());
        if (index < 0 || index >= updated.size()) {
            throw new IndexOutOfBoundsException("Message index out of range: " + index);
        }
        updated.set(index, newMessage);
        updateMessages(id, updated);
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

    public void updateMessages(AnnouncementId id, List<String> messages) {
        ensureOpen();
        Announcement current = requireDraft(id);
        Instant now = Instant.now();
        Announcement updated = new Announcement(
                current.id(),
                current.name(),
                current.enabled(),
                current.type(),
                current.channels(),
                messages,
                current.permission(),
                current.targetServers(),
                current.targetGroups(),
                current.interval(),
                current.cronExpression(),
                current.priority(),
                current.soundOptions(),
                current.toastOptions(),
                current.bossBarOptions(),
                current.actionBarOptions(),
                current.titleOptions(),
                current.conditions(),
                current.metadata(),
                current.createdAt(),
                now);
        drafts.put(id, updated);
    }

    public void addMessage(AnnouncementId id, String message) {
        ensureOpen();
        Announcement current = requireDraft(id);
        List<String> updated = new java.util.ArrayList<>(current.messages());
        updated.add(message);
        updateMessages(id, updated);
    }

    public void removeMessage(AnnouncementId id, int index) {
        ensureOpen();
        Announcement current = requireDraft(id);
        List<String> updated = new java.util.ArrayList<>(current.messages());
        if (index < 0 || index >= updated.size()) {
            throw new IndexOutOfBoundsException("Message index out of range: " + index);
        }
        updated.remove(index);
        if (updated.isEmpty()) {
            updated.add("<gray>Sin mensajes</gray>");
        }
        updateMessages(id, updated);
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
