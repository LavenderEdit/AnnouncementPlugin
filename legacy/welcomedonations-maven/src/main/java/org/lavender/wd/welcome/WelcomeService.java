package org.lavender.wd.welcome;

import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.lavender.wd.core.Colorizer;
import org.lavender.wd.core.PluginSettings;
import org.lavender.wd.core.PluginSettings.JoinTemplate;
import org.lavender.wd.core.PluginSettings.TitleTemplate;

/**
 *
 * @author Studios TKOH!
 */
public class WelcomeService {

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final Colorizer colorizer;

    public WelcomeService(JavaPlugin plugin, PluginSettings settings, Colorizer colorizer) {
        this.plugin = plugin;
        this.settings = settings;
        this.colorizer = colorizer;
    }

    public void handleJoin(Player player) {
        if (!settings.welcome().enabled()) {
            return;
        }
        boolean firstJoin = !player.hasPlayedBefore();
        JoinTemplate template = firstJoin ? settings.welcome().firstJoin() : settings.welcome().rejoin();
        sendChat(template.chat(), player);
        sendTitle(template.title(), player);
        playSound(template.sound(), player);
    }

    private void sendChat(String template, Player player) {
        if (template == null || template.isEmpty()) {
            return;
        }
        String message = template.replace("%player%", player.getName());
        Bukkit.broadcastMessage(colorizer.colorize(message));
    }

    private void sendTitle(TitleTemplate titleTemplate, Player player) {
        if (titleTemplate == null || !titleTemplate.enabled()) {
            return;
        }
        String main = colorizer.colorize(titleTemplate.main().replace("%player%", player.getName()));
        String sub = colorizer.colorize(titleTemplate.sub().replace("%player%", player.getName()));
        player.sendTitle(main, sub, titleTemplate.fadeIn(), titleTemplate.stay(), titleTemplate.fadeOut());
    }

    private void playSound(String soundName, Player player) {
        if (soundName == null || soundName.isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Sonido de bienvenida inv\u00e1lido: {0}", soundName);
        }
    }
}
