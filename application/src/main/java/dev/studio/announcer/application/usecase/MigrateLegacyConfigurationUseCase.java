package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.service.ConfigurationReloadResult;
import dev.studio.announcer.api.service.LegacyMigrationService;
import java.util.Objects;

public final class MigrateLegacyConfigurationUseCase {
    private final LegacyMigrationService migrationService;

    public MigrateLegacyConfigurationUseCase(LegacyMigrationService migrationService) {
        this.migrationService = Objects.requireNonNull(migrationService, "migrationService");
    }

    public ConfigurationReloadResult migrate() {
        return migrationService.migrate();
    }
}
