package dev.studio.announcer.api.service;

import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.Optional;

public interface AnnouncementManagementService {

    AnnouncementManagementResult create(Announcement announcement);

    AnnouncementManagementResult update(Announcement announcement);

    AnnouncementManagementResult toggle(AnnouncementId id);

    AnnouncementManagementResult delete(AnnouncementId id);

    AnnouncementManagementResult duplicate(AnnouncementId sourceId, AnnouncementId targetId);

    AnnouncementManagementResult saveAll();

    AnnouncementManagementResult validate(Announcement announcement);

    Optional<Announcement> findById(AnnouncementId id);
}
