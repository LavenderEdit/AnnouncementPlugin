package org.lavender.wd.donations;

import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.lavender.wd.core.PluginSettings;
import org.lavender.wd.donations.PackageDefinition.PackageType;
/**
 *
 * @author Studios TKOH!
 */
public class PackageCatalog {
    
    private final PluginSettings settings;

    public PackageCatalog(PluginSettings settings) {
        this.settings = settings;
    }

    public Optional<PackageDefinition> find(String packageId) {
        ConfigurationSection base = settings.packagesSection();
        if (base == null) {
            return Optional.empty();
        }
        ConfigurationSection section = base.getConfigurationSection(packageId);
        if (section == null) {
            return Optional.empty();
        }

        String typeValue = section.getString("type", "donation").toLowerCase();
        PackageType type = "rank".equals(typeValue) ? PackageType.RANK : PackageType.DONATION;

        String display = section.getString("label", packageId);
        String rankName = section.getString("rank", packageId);
        String format = section.getString("broadcast-format", type == PackageType.RANK
                ? settings.ranks().format()
                : settings.donations().format());
        boolean forward = section.getBoolean("auto-forward-proxy", false);
        String giveCommand = section.getString("give-command", "");

        return Optional.of(new PackageDefinition(packageId, type, display, rankName, format, forward, giveCommand));
    }
}
