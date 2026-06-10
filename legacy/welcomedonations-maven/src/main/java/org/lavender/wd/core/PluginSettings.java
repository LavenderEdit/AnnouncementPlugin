package org.lavender.wd.core;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.lavender.wd.WelcomeDonationsPlugin.ServerRole;

/**
 *
 * @author Studios TKOH!
 */
public class PluginSettings {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final WelcomeSettings welcomeSettings;
    private final DonationSettings donationSettings;
    private final RankSettings rankSettings;

    public PluginSettings(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.welcomeSettings = new WelcomeSettings(config);
        this.donationSettings = new DonationSettings(config);
        this.rankSettings = new RankSettings(config);
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public ServerRole serverRole() {
        String roleStr = config.getString("server-role", ServerRole.SPAWN.name()).toUpperCase(Locale.ROOT);
        try {
            return ServerRole.valueOf(roleStr);
        } catch (IllegalArgumentException ex) {
            return ServerRole.SPAWN;
        }
    }

    public String message(String path, String def) {
        return config.getString("messages." + path, def);
    }

    public WelcomeSettings welcome() {
        return welcomeSettings;
    }

    public DonationSettings donations() {
        return donationSettings;
    }

    public RankSettings ranks() {
        return rankSettings;
    }

    public ConfigurationSection packagesSection() {
        return config.getConfigurationSection("packages");
    }

    public static class WelcomeSettings {

        private final FileConfiguration config;

        WelcomeSettings(FileConfiguration config) {
            this.config = config;
        }

        public boolean enabled() {
            return config.getBoolean("welcome.enabled", true);
        }

        public JoinTemplate firstJoin() {
            return new JoinTemplate(config, "welcome.first-join");
        }

        public JoinTemplate rejoin() {
            return new JoinTemplate(config, "welcome.rejoin");
        }
    }

    public static class JoinTemplate {

        private final FileConfiguration config;
        private final String basePath;

        JoinTemplate(FileConfiguration config, String basePath) {
            this.config = config;
            this.basePath = basePath;
        }

        public String chat() {
            return config.getString(basePath + ".chat", "");
        }

        public TitleTemplate title() {
            return new TitleTemplate(config, basePath + ".title");
        }

        public String sound() {
            return config.getString(basePath + ".sound", "");
        }
    }

    public static class TitleTemplate {

        private final FileConfiguration config;
        private final String basePath;

        TitleTemplate(FileConfiguration config, String basePath) {
            this.config = config;
            this.basePath = basePath;
        }

        public boolean enabled() {
            return config.getBoolean(basePath + ".enabled", false);
        }

        public String main() {
            return config.getString(basePath + ".main", "");
        }

        public String sub() {
            return config.getString(basePath + ".sub", "");
        }

        public int fadeIn() {
            return config.getInt(basePath + ".fade-in", 10);
        }

        public int stay() {
            return config.getInt(basePath + ".stay", 50);
        }

        public int fadeOut() {
            return config.getInt(basePath + ".fade-out", 10);
        }
    }

    public static class DonationSettings {

        private final FileConfiguration config;

        DonationSettings(FileConfiguration config) {
            this.config = config;
        }

        public boolean enabled() {
            return config.getBoolean("donations.enabled", true);
        }

        public String format() {
            return config.getString("donations.format", "&6[Donación] &e%player% &fapoyó con &b%item% &f%amount%");
        }

        public String sound() {
            return config.getString("donations.sound", "");
        }

        public boolean proxyForwardEnabled() {
            return config.getBoolean("donations.proxy-forward.enabled", false);
        }
    }

    public static class RankSettings {

        private final FileConfiguration config;

        RankSettings(FileConfiguration config) {
            this.config = config;
        }

        public boolean enabled() {
            return config.getBoolean("ranks.enabled", true);
        }

        public String format() {
            return config.getString("ranks.format", "&6[Rango] &e%player% &fcompró &b%rank% ¡Bienvenido!");
        }

        public String sound() {
            return config.getString("ranks.sound", "");
        }

        public boolean proxyForwardEnabled() {
            return config.getBoolean("ranks.proxy-forward.enabled", false);
        }

        public List<String> allowedRanks() {
            List<String> ranks = config.getStringList("ranks.allowed-ranks");
            return ranks == null ? Collections.emptyList() : ranks;
        }
    }
}
