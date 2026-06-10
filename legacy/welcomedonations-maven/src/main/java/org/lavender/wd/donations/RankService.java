package org.lavender.wd.donations;

import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.lavender.wd.core.Colorizer;
import org.lavender.wd.core.PluginSettings;
import org.lavender.wd.proxy.ProxyMessenger;

/**
 *
 * @author Studios TKOH!
 */
public class RankService {

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final Colorizer colorizer;
    private final ProxyMessenger proxyMessenger;

    public RankService(JavaPlugin plugin, PluginSettings settings, Colorizer colorizer, ProxyMessenger proxyMessenger) {
        this.plugin = plugin;
        this.settings = settings;
        this.colorizer = colorizer;
        this.proxyMessenger = proxyMessenger;
    }

    public void broadcast(RankPurchase purchase) {
        broadcast(purchase, false, null);
    }

    public void broadcast(RankPurchase purchase, boolean forceProxyForward, String formatOverride) {
        announceLocal(purchase, formatOverride);
        if (proxyMessenger != null && (forceProxyForward || settings.ranks().proxyForwardEnabled())) {
            proxyMessenger.sendRank(purchase);
        }
    }

    public void announceLocal(RankPurchase purchase) {
        announceLocal(purchase, null);
    }

    public void announceLocal(RankPurchase purchase, String formatOverride) {
        if (!settings.ranks().enabled()) {
            return;
        }
        String format = formatOverride != null ? formatOverride : settings.ranks().format();
        String message = format
                .replace("%player%", purchase.player())
                .replace("%rank%", purchase.rank())
                .replace("%amount%", purchase.amountOrEmpty());
        Bukkit.broadcastMessage(colorizer.colorize(message));

        String soundName = settings.ranks().sound();
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, 1f, 1f);
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().log(Level.WARNING, "Sonido de rango inv\u00e1lido: {0}", soundName);
            }
        }
    }

    public void executeGiveCommand(RankPurchase purchase, String commandTemplate) {
        if (commandTemplate == null || commandTemplate.isBlank()) {
            return;
        }
        String command = commandTemplate
                .replace("%player%", purchase.player())
                .replace("%rank%", purchase.rank())
                .replace("%amount%", purchase.amountOrEmpty());
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
