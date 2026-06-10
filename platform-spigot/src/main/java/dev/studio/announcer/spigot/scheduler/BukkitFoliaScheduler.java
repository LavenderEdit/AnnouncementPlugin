package dev.studio.announcer.spigot.scheduler;

import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitFoliaScheduler extends PlatformSchedulerAdapter {

    public BukkitFoliaScheduler(JavaPlugin plugin) {
        super(new BukkitSchedulerBackend(plugin));
    }
}
