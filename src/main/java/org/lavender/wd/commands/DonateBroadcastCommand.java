package org.lavender.wd.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.lavender.wd.WelcomeDonationsPlugin;

/**
 *
 * @authors Lavender & VaCris
 */
public class DonateBroadcastCommand implements CommandExecutor, TabCompleter {

    private final WelcomeDonationsPlugin plugin;

    public DonateBroadcastCommand(WelcomeDonationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wd.donate.broadcast")) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.usage-donate")));
            return true;
        }

        String player = args[0];
        String item = args[1];
        String amount = (args.length >= 3) ? args[2] : "";

        ConfigurationSection pkgSec = plugin.getConfig().getConfigurationSection("packages." + item);
        if (pkgSec != null) {
            String type = pkgSec.getString("type", "donation").toLowerCase();

            if ("rank".equals(type)) {
                String rankName = pkgSec.getString("rank", item);
                String fmt = pkgSec.getString("broadcast-format", plugin.getConfig().getString("ranks.format", "&6[Rango] &e%player% &fcompró &b%rank%"));
                String msg = fmt.replace("%player%", player).replace("%rank%", rankName).replace("%amount%", amount);
                Bukkit.broadcastMessage(color(msg));

                String giveCmd = pkgSec.getString("give-command", "");
                if (giveCmd != null && !giveCmd.isBlank()) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), giveCmd.replace("%player%", player).replace("%rank%", rankName).replace("%amount%", amount));
                }

                if (pkgSec.getBoolean("auto-forward-proxy", false)) {
                    plugin.forwardRankViaProxy(player, rankName);
                }
            } else {
                String labelPkg = pkgSec.getString("label", item);
                String fmt = pkgSec.getString("broadcast-format", plugin.getConfig().getString("donations.format", "&6[Donación] &e%player% &fapoyó con &b%amount%"));
                String msg = fmt.replace("%player%", player).replace("%item%", labelPkg).replace("%amount%", amount).replace("%monto%", amount);
                Bukkit.broadcastMessage(color(msg));

                if (pkgSec.getBoolean("auto-forward-proxy", false)) {
                    plugin.forwardDonationViaProxy(player, labelPkg, amount);
                }
            }
        } else {
            List<String> allowedRanks = plugin.getConfig().getStringList("ranks.allowed-ranks");
            if (allowedRanks.contains(item)) {
                plugin.broadcastDonation(player, "Rango " + item, "");
            } else {
                plugin.broadcastDonation(player, item, amount);
            }
        }
        return true;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    // Tab complete (opcional)
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("wd.donate.broadcast")) {
            return Collections.emptyList();
        }

        switch (args.length) {
            case 1 -> {
                // sugerir jugadores online
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(p -> p.getName())
                        .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            case 2 -> {
                // sugerir paquetes comunes
                ConfigurationSection pkgs = plugin.getConfig().getConfigurationSection("packages");
                if (pkgs == null) {
                    return Collections.emptyList();
                }
                List<String> sugerencias = new ArrayList<>(pkgs.getKeys(false));
                return filtrar(sugerencias, args[1]);
            }
            case 3 -> {
                // sugerir montos
                List<String> montos = List.of("5USD", "10USD", "20USD");
                return filtrar(montos, args[2]);
            }
            default -> {
            }
        }

        return Collections.emptyList();
    }

    private List<String> filtrar(List<String> base, String pref) {
        String p = pref.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : base) {
            if (s.toLowerCase().startsWith(p)) {
                out.add(s);
            }
        }
        return out;
    }

}
