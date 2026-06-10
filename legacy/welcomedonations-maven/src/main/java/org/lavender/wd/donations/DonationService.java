package org.lavender.wd.donations;

import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.lavender.wd.core.Colorizer;
import org.lavender.wd.core.MessageManager;
import org.lavender.wd.core.PluginSettings;
import org.lavender.wd.proxy.ProxyMessenger;

/**
 *
 * @author Studios TKOH!
 */
public class DonationService {

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final Colorizer colorizer;
    private final MessageManager messageManager;
    private final ProxyMessenger proxyMessenger;

    public DonationService(JavaPlugin plugin, PluginSettings settings, Colorizer colorizer, MessageManager messageManager, ProxyMessenger proxyMessenger) {
        this.plugin = plugin;
        this.settings = settings;
        this.colorizer = colorizer;
        this.messageManager = messageManager;
        this.proxyMessenger = proxyMessenger;
    }

    public void broadcast(DonationAnnouncement announcement) {
        broadcast(announcement, false, null);
    }

    public void broadcast(DonationAnnouncement announcement, boolean forceProxyForward, String formatOverride) {
        announceLocal(announcement, formatOverride);
        if (proxyMessenger != null && (forceProxyForward || settings.donations().proxyForwardEnabled())) {
            proxyMessenger.sendDonation(announcement);
        }
    }

    public void announceLocal(DonationAnnouncement announcement) {
        announceLocal(announcement, null);
    }

    public void announceLocal(DonationAnnouncement announcement, String formatOverride) {
        if (!settings.donations().enabled()) {
            return;
        }

        String format = formatOverride != null ? formatOverride : settings.donations().format();

        String message = format
                .replace("%player%", announcement.player())
                .replace("%item%", announcement.item())
                .replace("%amount%", announcement.amountOrEmpty())
                .replace("%monto%", announcement.amountOrEmpty());

        messageManager.broadcast(message);

        String soundName = settings.donations().sound();
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, 1f, 1f);
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().log(Level.WARNING, "Sonido de donación inválido: {0}", soundName);
            }
        }
    }
}
