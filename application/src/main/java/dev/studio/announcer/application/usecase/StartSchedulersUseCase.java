package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.AnnouncementSchedulerService;
import java.util.Objects;

public final class StartSchedulersUseCase {
    private final AnnouncementRepository repository;
    private final AnnouncementSchedulerService schedulerService;

    public StartSchedulersUseCase(
            AnnouncementRepository repository,
            AnnouncementSchedulerService schedulerService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.schedulerService = Objects.requireNonNull(schedulerService, "schedulerService");
    }

    public void start() {
        schedulerService.reschedule(repository.findAll());
    }
}
