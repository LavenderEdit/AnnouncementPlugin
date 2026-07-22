package dev.studio.announcer.application.service;

import dev.studio.announcer.api.service.AnnouncementManagementResult;
import dev.studio.announcer.api.service.AnnouncementManagementService;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.AnnouncementSchedulerService;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import dev.studio.announcer.domain.announcement.AnnouncementValidator;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DefaultAnnouncementManagementService implements AnnouncementManagementService {
    private final AnnouncementRepository repository;
    private final AnnouncementSchedulerService schedulerService;
    private final AnnouncementValidator validator;

    public DefaultAnnouncementManagementService(
            AnnouncementRepository repository,
            AnnouncementSchedulerService schedulerService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.schedulerService = Objects.requireNonNull(schedulerService, "schedulerService");
        this.validator = new AnnouncementValidator();
    }

    @Override
    public AnnouncementManagementResult create(Announcement announcement) {
        if (announcement == null) {
            return AnnouncementManagementResult.failure("Announcement cannot be null.");
        }
        if (repository.findById(announcement.id()).isPresent()) {
            return AnnouncementManagementResult.failure("Announcement '" + announcement.id().value() + "' already exists.");
        }
        ValidationResult validation = validator.validate(announcement);
        if (!validation.valid()) {
            return AnnouncementManagementResult.validationFailure(validation);
        }
        repository.save(announcement);
        schedulerService.schedule(announcement);
        return AnnouncementManagementResult.success("Created announcement '" + announcement.id().value() + "'.");
    }

    @Override
    public AnnouncementManagementResult update(Announcement announcement) {
        if (announcement == null) {
            return AnnouncementManagementResult.failure("Announcement cannot be null.");
        }
        if (repository.findById(announcement.id()).isEmpty()) {
            return AnnouncementManagementResult.failure("Announcement not found: " + announcement.id().value());
        }
        ValidationResult validation = validator.validate(announcement);
        if (!validation.valid()) {
            return AnnouncementManagementResult.validationFailure(validation);
        }
        Announcement updated = new Announcement(
                announcement.id(),
                announcement.name(),
                announcement.enabled(),
                announcement.type(),
                announcement.channels(),
                announcement.messages(),
                announcement.permission(),
                announcement.targetServers(),
                announcement.targetGroups(),
                announcement.interval(),
                announcement.cronExpression(),
                announcement.priority(),
                announcement.soundOptions(),
                announcement.toastOptions(),
                announcement.bossBarOptions(),
                announcement.actionBarOptions(),
                announcement.titleOptions(),
                announcement.conditions(),
                announcement.metadata(),
                announcement.createdAt(),
                Instant.now());
        repository.save(updated);
        if (updated.enabled() && updated.interval().isPresent()) {
            schedulerService.schedule(updated);
        } else {
            schedulerService.cancel(updated.id());
        }
        return AnnouncementManagementResult.success("Updated announcement '" + updated.id().value() + "'.");
    }

    @Override
    public AnnouncementManagementResult toggle(AnnouncementId id) {
        if (id == null) {
            return AnnouncementManagementResult.failure("Announcement ID cannot be null.");
        }
        Announcement current = repository.findById(id)
                .orElse(null);
        if (current == null) {
            return AnnouncementManagementResult.failure("Announcement not found: " + id.value());
        }
        Announcement toggled = current.withEnabled(!current.enabled());
        repository.save(toggled);
        if (toggled.enabled() && toggled.interval().isPresent()) {
            schedulerService.schedule(toggled);
        } else {
            schedulerService.cancel(id);
        }
        return AnnouncementManagementResult.success("Announcement '" + id.value() + "' "
                + (toggled.enabled() ? "enabled" : "disabled") + ".");
    }

    @Override
    public AnnouncementManagementResult delete(AnnouncementId id) {
        if (id == null) {
            return AnnouncementManagementResult.failure("Announcement ID cannot be null.");
        }
        if (!repository.deleteById(id)) {
            return AnnouncementManagementResult.failure("Announcement not found: " + id.value());
        }
        schedulerService.cancel(id);
        return AnnouncementManagementResult.success("Deleted announcement '" + id.value() + "'.");
    }

    @Override
    public AnnouncementManagementResult duplicate(AnnouncementId sourceId, AnnouncementId targetId) {
        if (sourceId == null || targetId == null) {
            return AnnouncementManagementResult.failure("Source and target IDs cannot be null.");
        }
        Announcement source = repository.findById(sourceId).orElse(null);
        if (source == null) {
            return AnnouncementManagementResult.failure("Source announcement not found: " + sourceId.value());
        }
        if (repository.findById(targetId).isPresent()) {
            return AnnouncementManagementResult.failure("Target announcement already exists: " + targetId.value());
        }
        Instant now = Instant.now();
        Announcement copy = new Announcement(
                targetId,
                source.name() + " Copy",
                source.enabled(),
                source.type(),
                EnumSet.copyOf(source.channels()),
                List.copyOf(source.messages()),
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
                List.copyOf(source.conditions()),
                source.metadata(),
                now,
                now);
        ValidationResult validation = validator.validate(copy);
        if (!validation.valid()) {
            return AnnouncementManagementResult.validationFailure(validation);
        }
        repository.save(copy);
        if (copy.enabled() && copy.interval().isPresent()) {
            schedulerService.schedule(copy);
        }
        return AnnouncementManagementResult.success("Duplicated announcement '" + sourceId.value() + "' to '" + targetId.value() + "'.");
    }

    @Override
    public AnnouncementManagementResult saveAll() {
        schedulerService.reschedule(repository.findAll());
        return AnnouncementManagementResult.success("All announcements saved and scheduler updated.");
    }

    @Override
    public AnnouncementManagementResult validate(Announcement announcement) {
        if (announcement == null) {
            return AnnouncementManagementResult.failure("Announcement cannot be null.");
        }
        ValidationResult result = validator.validate(announcement);
        if (result.valid()) {
            return AnnouncementManagementResult.success("Announcement is valid.");
        }
        return AnnouncementManagementResult.validationFailure(result);
    }

    @Override
    public Optional<Announcement> findById(AnnouncementId id) {
        return repository.findById(id);
    }
}
