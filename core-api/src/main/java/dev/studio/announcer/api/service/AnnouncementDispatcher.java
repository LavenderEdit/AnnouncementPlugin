package dev.studio.announcer.api.service;

import dev.studio.announcer.domain.announcement.Announcement;

public interface AnnouncementDispatcher {

    DeliverySummary broadcast(Announcement announcement);

    DeliverySummary preview(Announcement announcement, String audienceId);
}
