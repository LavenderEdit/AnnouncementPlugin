package dev.studio.announcer.api.service;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository {

    Announcement save(Announcement announcement);

    Optional<Announcement> findById(AnnouncementId id);

    List<Announcement> findAll();

    boolean deleteById(AnnouncementId id);
}
