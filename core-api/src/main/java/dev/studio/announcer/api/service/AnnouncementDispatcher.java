package dev.studio.announcer.api.service;

import dev.studio.announcer.api.audience.Audience;
import dev.studio.announcer.domain.announcement.Announcement;

public interface AnnouncementDispatcher {

    default DeliverySummary broadcast(Announcement announcement) {
        return dispatch(announcement, Audience.all(), null);
    }

    default DeliverySummary preview(Announcement announcement, String audienceId) {
        return dispatch(announcement, Audience.player(audienceId), null);
    }

    default DeliverySummary dispatch(Announcement announcement, String actorId) {
        return dispatch(announcement, Audience.fromMetadata(announcement), actorId);
    }

    default DeliverySummary dispatch(Announcement announcement, Audience audience, String actorId) {
        return DeliverySummary.empty();
    }
}
