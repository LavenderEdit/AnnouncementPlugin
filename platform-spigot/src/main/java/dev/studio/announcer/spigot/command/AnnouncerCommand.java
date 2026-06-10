package dev.studio.announcer.spigot.command;

import dev.studio.announcer.application.command.AnnouncerCommandService;
import dev.studio.announcer.application.command.CommandOutcome;
import dev.studio.announcer.application.command.CommandRequest;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AnnouncerCommand implements CommandExecutor, TabCompleter {
    private final AnnouncerCommandService commandService;

    public AnnouncerCommand(AnnouncerCommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("announcer.admin")) {
            sender.sendMessage("You do not have permission to use AdvancedAnnouncer.");
            return true;
        }
        CommandOutcome outcome = commandService.handle(request(sender, args));
        outcome.messages().forEach(sender::sendMessage);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("announcer.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return commandService.suggestions(args[0]);
        }
        return List.of();
    }

    private CommandRequest request(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            UUID id = player.getUniqueId();
            return CommandRequest.player(id.toString(), args);
        }
        return CommandRequest.console(args);
    }
}
