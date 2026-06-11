package dev.studio.announcer.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class ConfigBackupService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private final Path backupDirectory;
    private final Clock clock;

    public ConfigBackupService(Path backupDirectory) {
        this(backupDirectory, Clock.systemUTC());
    }

    public ConfigBackupService(Path backupDirectory, Clock clock) {
        this.backupDirectory = Objects.requireNonNull(backupDirectory, "backupDirectory");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Path backup(Path source) {
        try {
            if (source == null || Files.notExists(source)) {
                throw new IllegalArgumentException("Cannot backup missing file: " + source);
            }
            Files.createDirectories(backupDirectory);
            String fileName = source.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            String base = dot > 0 ? fileName.substring(0, dot) : fileName;
            String extension = dot > 0 ? fileName.substring(dot) : "";
            Path backup = backupDirectory.resolve(base + "-" + FORMATTER.format(Instant.now(clock)) + extension);
            int collision = 1;
            while (Files.exists(backup)) {
                backup = backupDirectory.resolve(base + "-" + FORMATTER.format(Instant.now(clock)) + "-" + collision + extension);
                collision++;
            }
            Files.copy(source, backup);
            return backup;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create backup for " + source, ex);
        }
    }
}
