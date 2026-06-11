package dev.studio.announcer.common.config;

import dev.studio.announcer.api.service.ConfigurationReloadResult;
import dev.studio.announcer.api.service.ConfigurationReloadService;
import dev.studio.announcer.api.service.AnnouncementSchedulerService;
import dev.studio.announcer.domain.validation.ValidationResult;
import java.util.List;
import java.util.Objects;

public final class YamlConfigurationReloadService implements ConfigurationReloadService {
    private final ConfigurationPaths paths;
    private final YamlAnnouncementRepository runtimeRepository;
    private final ConfigurationValidationService validationService;
    private final AnnouncementSchedulerService schedulerService;
    private final Runnable beforeReload;

    public YamlConfigurationReloadService(
            ConfigurationPaths paths,
            YamlAnnouncementRepository runtimeRepository,
            ConfigurationValidationService validationService) {
        this(paths, runtimeRepository, validationService, null);
    }

    public YamlConfigurationReloadService(
            ConfigurationPaths paths,
            YamlAnnouncementRepository runtimeRepository,
            ConfigurationValidationService validationService,
            AnnouncementSchedulerService schedulerService) {
        this(paths, runtimeRepository, validationService, schedulerService, () -> {
        });
    }

    public YamlConfigurationReloadService(
            ConfigurationPaths paths,
            YamlAnnouncementRepository runtimeRepository,
            ConfigurationValidationService validationService,
            AnnouncementSchedulerService schedulerService,
            Runnable beforeReload) {
        this.paths = Objects.requireNonNull(paths, "paths");
        this.runtimeRepository = Objects.requireNonNull(runtimeRepository, "runtimeRepository");
        this.validationService = Objects.requireNonNull(validationService, "validationService");
        this.schedulerService = schedulerService;
        this.beforeReload = beforeReload == null ? () -> {
        } : beforeReload;
    }

    @Override
    public ConfigurationReloadResult reload() {
        try {
            beforeReload.run();
            new ConfigurationBootstrapper(paths).bootstrap();
            YamlAnnouncementRepository candidate = new YamlAnnouncementRepository(paths.announcementsDirectory());
            ValidationResult validation = validationService.validateAnnouncements(candidate.findAll());
            if (!validation.valid()) {
                return ConfigurationReloadResult.failure("Reload failed.", validation.errors());
            }
            runtimeRepository.replaceWith(candidate);
            if (schedulerService != null) {
                schedulerService.reschedule(runtimeRepository.findAll());
            }
            return ConfigurationReloadResult.success("Reloaded " + runtimeRepository.findAll().size() + " announcement(s).");
        } catch (RuntimeException ex) {
            return ConfigurationReloadResult.failure(
                    "Reload failed.",
                    List.of(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }
}
