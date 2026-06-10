package dev.studio.announcer.common.repository;

import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryAnnouncementRepository implements AnnouncementRepository {
    private final ConcurrentMap<AnnouncementId, Announcement> announcements = new ConcurrentHashMap<>();

    @Override
    public Announcement save(Announcement announcement) {
        announcements.put(announcement.id(), announcement);
        return announcement;
    }

    @Override
    public Optional<Announcement> findById(AnnouncementId id) {
        return Optional.ofNullable(announcements.get(id));
    }

    @Override
    public List<Announcement> findAll() {
        return announcements.values().stream()
                .sorted(Comparator.comparing(announcement -> announcement.id().value()))
                .toList();
    }

    @Override
    public boolean deleteById(AnnouncementId id) {
        return announcements.remove(id) != null;
    }
}
