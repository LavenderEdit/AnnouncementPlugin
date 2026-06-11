package dev.studio.announcer.common.config;

import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class YamlAnnouncementRepository implements AnnouncementRepository {
    private static final String DEFAULT_FILE = "global.yml";

    private final Path announcementsDirectory;
    private final Map<AnnouncementId, StoredAnnouncement> announcements = new ConcurrentHashMap<>();

    public YamlAnnouncementRepository(Path announcementsDirectory) {
        this.announcementsDirectory = announcementsDirectory;
        reload();
    }

    public synchronized void reload() {
        announcements.clear();
        announcements.putAll(loadAll());
    }

    synchronized void replaceWith(YamlAnnouncementRepository other) {
        announcements.clear();
        announcements.putAll(other.announcements);
    }

    private Map<AnnouncementId, StoredAnnouncement> loadAll() {
        Map<AnnouncementId, StoredAnnouncement> loaded = new ConcurrentHashMap<>();
        try {
            Files.createDirectories(announcementsDirectory);
            try (var files = Files.list(announcementsDirectory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".yml"))
                        .sorted()
                        .forEach(file -> loadFile(file, loaded));
            }
            return loaded;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not reload announcement YAML files.", ex);
        }
    }

    @Override
    public synchronized Announcement save(Announcement announcement) {
        Path file = announcements.getOrDefault(
                        announcement.id(),
                        new StoredAnnouncement(announcement, announcementsDirectory.resolve(DEFAULT_FILE)))
                .file();
        try {
            Files.createDirectories(file.getParent());
            YamlConfigurationLoader loader = loader(file);
            ConfigurationNode root = Files.exists(file) ? loader.load() : loader.createNode();
            AnnouncementYamlMapper.write(announcement, root.node("announcements", announcement.id().value()));
            loader.save(root);
            announcements.put(announcement.id(), new StoredAnnouncement(announcement, file));
            return announcement;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save announcement '" + announcement.id().value() + "'.", ex);
        }
    }

    @Override
    public Optional<Announcement> findById(AnnouncementId id) {
        StoredAnnouncement stored = announcements.get(id);
        return stored == null ? Optional.empty() : Optional.of(stored.announcement());
    }

    @Override
    public List<Announcement> findAll() {
        return announcements.values().stream()
                .map(StoredAnnouncement::announcement)
                .sorted(Comparator.comparing(announcement -> announcement.id().value()))
                .toList();
    }

    @Override
    public synchronized boolean deleteById(AnnouncementId id) {
        StoredAnnouncement removed = announcements.remove(id);
        if (removed == null) {
            return false;
        }
        try {
            YamlConfigurationLoader loader = loader(removed.file());
            ConfigurationNode root = loader.load();
            root.node("announcements").removeChild(id.value());
            loader.save(root);
            return true;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete announcement '" + id.value() + "'.", ex);
        }
    }

    public Map<AnnouncementId, Path> index() {
        Map<AnnouncementId, Path> index = new LinkedHashMap<>();
        announcements.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(AnnouncementId::value)))
                .forEach(entry -> index.put(entry.getKey(), entry.getValue().file()));
        return Map.copyOf(index);
    }

    private void loadFile(Path file, Map<AnnouncementId, StoredAnnouncement> target) {
        try {
            ConfigurationNode root = loader(file).load();
            ConfigurationNode announcementsNode = root.node("announcements");
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : announcementsNode.childrenMap().entrySet()) {
                String id = String.valueOf(entry.getKey());
                Announcement announcement = AnnouncementYamlMapper.read(id, entry.getValue());
                target.put(announcement.id(), new StoredAnnouncement(announcement, file));
            }
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Could not load announcements from " + file, ex);
        }
    }

    private YamlConfigurationLoader loader(Path file) {
        return YamlConfigurationLoader.builder().path(file).build();
    }

    private record StoredAnnouncement(Announcement announcement, Path file) {
    }
}
