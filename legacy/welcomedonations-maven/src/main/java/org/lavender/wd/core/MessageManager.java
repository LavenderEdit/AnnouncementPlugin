package org.lavender.wd.core;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Studios TKOH!
 */
public class MessageManager {

    private final BukkitAudiences adventure;
    private final MiniMessage miniMessage;

    public MessageManager(JavaPlugin plugin) {
        this.adventure = BukkitAudiences.create(plugin);
        this.miniMessage = MiniMessage.miniMessage();
    }

    public Component parse(String input) {
        if (input == null) {
            return Component.empty();
        }
        return miniMessage.deserialize(input);
    }

    public void send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        adventure.sender(sender).sendMessage(parse(message));
    }

    public void send(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        adventure.player(player).sendMessage(parse(message));
    }

    public void broadcast(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        adventure.all().sendMessage(parse(message));
    }

    public void close() {
        if (this.adventure != null) {
            this.adventure.close();
        }
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}
