package org.lavender.wd.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.lavender.wd.core.Colorizer;
import org.lavender.wd.core.PluginSettings;
import org.lavender.wd.donations.DonationAnnouncement;
import org.lavender.wd.donations.DonationService;
import org.lavender.wd.donations.PackageCatalog;
import org.lavender.wd.donations.PackageDefinition;
import org.lavender.wd.donations.RankPurchase;
import org.lavender.wd.donations.RankService;

/**
 *
 * @authors Studios TKOH!
 */
public class DonateBroadcastCommand implements CommandExecutor, TabCompleter {

    private final PluginSettings settings;
    private final Colorizer colorizer;
    private final PackageCatalog packageCatalog;
    private final DonationService donationService;
    private final RankService rankService;

    public DonateBroadcastCommand(PluginSettings settings, Colorizer colorizer, PackageCatalog packageCatalog,
            DonationService donationService, RankService rankService) {
        this.settings = settings;
        this.colorizer = colorizer;
        this.packageCatalog = packageCatalog;
        this.donationService = donationService;
        this.rankService = rankService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wd.donate.broadcast")) {
            sender.sendMessage(colorizer.colorize(settings.message("no-permission", "&cNo tienes permiso.")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(colorizer.colorize(settings.message("usage-donate", "&eUso: /donatebroadcast <jugador> <paquete> [monto]")));
            return true;
        }

        String player = args[0];
        String packageId = args[1];
        String amount = args.length >= 3 ? args[2] : "";
        Optional<PackageDefinition> definitionOpt = packageCatalog.find(packageId);
        if (definitionOpt.isPresent()) {
            handleDefinedPackage(definitionOpt.get(), player, amount);
            return true;
        }
        if (settings.ranks().allowedRanks().contains(packageId)) {
            donationService.broadcast(new DonationAnnouncement(player, "Rango " + packageId, ""));
        } else {
            donationService.broadcast(new DonationAnnouncement(player, packageId, amount));
        }
        return true;
    }

    private void handleDefinedPackage(PackageDefinition definition, String player, String amount) {
        switch (definition.type()) {
            case RANK -> {
                String rankName = definition.rankName() == null || definition.rankName().isEmpty()
                        ? definition.displayLabel()
                        : definition.rankName();
                RankPurchase purchase = new RankPurchase(player, rankName, amount);
                rankService.broadcast(purchase, definition.autoForwardProxy(), definition.broadcastFormat());
                if (definition.hasGiveCommand()) {
                    rankService.executeGiveCommand(purchase, definition.giveCommand());
                }
            }
            case DONATION -> {
                String label = definition.displayLabel() == null || definition.displayLabel().isEmpty()
                        ? definition.id()
                        : definition.displayLabel();
                DonationAnnouncement announcement = new DonationAnnouncement(player, label, amount);
                donationService.broadcast(announcement, definition.autoForwardProxy(), definition.broadcastFormat());
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("wd.donate.broadcast")) {
            return Collections.emptyList();
        }
        return switch (args.length) {
            case 1 ->
                Bukkit.getOnlinePlayers().stream()
                .map(player -> player.getName())
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
            case 2 -> {
                ConfigurationSection section = settings.packagesSection();
                if (section == null) {
                    yield Collections.emptyList();
                }
                List<String> suggestions = new ArrayList<>(section.getKeys(false));
                suggestions.addAll(settings.ranks().allowedRanks());
                yield filterByPrefix(suggestions, args[1]);
            }
            case 3 ->
                filterByPrefix(List.of("5USD", "10USD", "20USD"), args[2]);
            default ->
                Collections.emptyList();
        };
    }

    private List<String> filterByPrefix(List<String> base, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : base) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
