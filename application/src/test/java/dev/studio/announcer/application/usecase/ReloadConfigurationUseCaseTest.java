package dev.studio.announcer.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.api.service.ConfigurationReloadResult;
import dev.studio.announcer.api.service.ConfigurationReloadService;
import org.junit.jupiter.api.Test;

class ReloadConfigurationUseCaseTest {

    @Test
    void returnsSuccessMessageFromReloadService() {
        ReloadConfigurationUseCase useCase = new ReloadConfigurationUseCase(
                () -> ConfigurationReloadResult.success("Reloaded 2 announcement(s)."));

        ConfigurationReloadResult result = useCase.reload();

        assertTrue(result.success());
        assertEquals("Reloaded 2 announcement(s).", result.message());
    }

    @Test
    void commandServiceCanReportReloadFailures() {
        ConfigurationReloadService service = () -> ConfigurationReloadResult.failure(
                "Reload failed.",
                java.util.List.of("Invalid MiniMessage"));
        ReloadConfigurationUseCase useCase = new ReloadConfigurationUseCase(service);

        ConfigurationReloadResult result = useCase.reload();

        assertEquals(java.util.List.of("Invalid MiniMessage"), result.errors());
    }
}
