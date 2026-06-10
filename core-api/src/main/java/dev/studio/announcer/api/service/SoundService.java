package dev.studio.announcer.api.service;

import dev.studio.announcer.domain.announcement.option.SoundOptions;

public interface SoundService {

    boolean validSound(String key);

    boolean play(String audienceId, SoundOptions options);
}
