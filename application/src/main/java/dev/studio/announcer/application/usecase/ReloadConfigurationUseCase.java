package dev.studio.announcer.application.usecase;

import dev.studio.announcer.api.service.ConfigurationReloadResult;
import dev.studio.announcer.api.service.ConfigurationReloadService;
import java.util.Objects;

public final class ReloadConfigurationUseCase {
    private final ConfigurationReloadService reloadService;

    public ReloadConfigurationUseCase(ConfigurationReloadService reloadService) {
        this.reloadService = Objects.requireNonNull(reloadService, "reloadService");
    }

    public ConfigurationReloadResult reload() {
        return reloadService.reload();
    }
}
