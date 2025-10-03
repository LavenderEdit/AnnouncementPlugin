package org.lavender.wd.core;

import org.bukkit.ChatColor;

/**
 *
 * @author Studios TKOH!
 */
public class Colorizer {

    public String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
