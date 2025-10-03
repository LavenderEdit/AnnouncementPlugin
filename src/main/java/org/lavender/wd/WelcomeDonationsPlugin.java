package org.lavender.wd;

import java.util.logging.Level;
import org.bukkit.Bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.lavender.wd.commands.DonateBroadcastCommand;
import org.lavender.wd.core.AuthWarningListener;
import org.lavender.wd.core.Colorizer;
import org.lavender.wd.core.PluginSettings;
import org.lavender.wd.donations.DonationService;
import org.lavender.wd.donations.PackageCatalog;
import org.lavender.wd.donations.RankService;
import org.lavender.wd.proxy.ProxyMessenger;
import org.lavender.wd.welcome.PlayerJoinListener;
import org.lavender.wd.welcome.WelcomeService;

/**
 *
 * @authors Studios TKOH!
 */
public class WelcomeDonationsPlugin extends JavaPlugin {

    public enum ServerRole {
        AUTH, SPAWN, SURVIVAL
    }

    private PluginSettings settings;
    private Colorizer colorizer;
    private ProxyMessenger proxyMessenger;
    private DonationService donationService;
    private RankService rankService;
    private PackageCatalog packageCatalog;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.colorizer = new Colorizer();
        this.settings = new PluginSettings(this);
        this.proxyMessenger = new ProxyMessenger(this);
        this.donationService = new DonationService(this, settings, colorizer, proxyMessenger);
        this.rankService = new RankService(this, settings, colorizer, proxyMessenger);
        this.packageCatalog = new PackageCatalog(settings);
        proxyMessenger.setDonationHandler(donationService::announceLocal);
        proxyMessenger.setRankHandler(rankService::announceLocal);
        proxyMessenger.register();
        if (settings.serverRole() == ServerRole.AUTH) {

            getLogger().info("Plugin deshabilitado en AUTH.");
            String message = settings.message("disabled-on-auth", "");
            AuthWarningListener listener = new AuthWarningListener(colorizer, message);
            getServer().getPluginManager().registerEvents(listener, this);
            if (message != null && !message.isEmpty()) {
                Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(colorizer.colorize(message)));

            }

            return;
        }
        WelcomeService welcomeService = new WelcomeService(this, settings, colorizer);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(welcomeService), this);
        var command = getCommand("ga");
        if (command != null) {
            DonateBroadcastCommand executor = new DonateBroadcastCommand(settings, colorizer, packageCatalog, donationService, rankService);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("El comando 'ga' no está definido en plugin.yml");

        }
        getLogger().log(Level.INFO, "WelcomeDonations habilitado en rol: {0}", settings.serverRole());
    }

    @Override
    public void onDisable() {
        if (proxyMessenger != null) {
            proxyMessenger.unregister();

        }
    }
}
