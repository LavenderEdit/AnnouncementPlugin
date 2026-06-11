package dev.studio.announcer.spigot.command;

import dev.studio.announcer.api.service.AnnouncementEditorService;
import dev.studio.announcer.application.command.AnnouncerCommandService;
import dev.studio.announcer.application.command.CommandOutcome;
import dev.studio.announcer.application.command.CommandRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AnnouncerCommand implements CommandExecutor, TabCompleter {
    private final AnnouncerCommandService commandService;
    private final AnnouncementEditorService editorService;

    public AnnouncerCommand(AnnouncerCommandService commandService) {
        this(commandService, audienceId -> {
        });
    }

    public AnnouncerCommand(AnnouncerCommandService commandService, AnnouncementEditorService editorService) {
        this.commandService = commandService;
        this.editorService = editorService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && "editor".equalsIgnoreCase(args[0])) {
            return openEditor(sender);
        }
        if (!hasCommandPermission(sender, args)) {
            sender.sendMessage("You do not have permission to use AdvancedAnnouncer.");
            return true;
        }
        CommandOutcome outcome = commandService.handle(request(sender, args));
        outcome.messages().forEach(sender::sendMessage);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = suggestionsFor(sender, args[0]);
            return suggestions.stream().distinct().sorted().toList();
        }
        return List.of();
    }

    private boolean openEditor(CommandSender sender) {
        if (!sender.hasPermission("announcer.editor") && !sender.hasPermission("announcer.admin")) {
            sender.sendMessage("You do not have permission to open the AdvancedAnnouncer editor.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Editor can only be opened in-game.");
            return true;
        }
        editorService.openEditor(player.getUniqueId().toString());
        return true;
    }

    private boolean hasCommandPermission(CommandSender sender, String[] args) {
        if (sender.hasPermission("announcer.admin")) {
            return true;
        }
        if (args.length == 0) {
            return false;
        }
        String permission = permissionFor(args[0]);
        return permission != null && sender.hasPermission(permission);
    }

    private List<String> suggestionsFor(CommandSender sender, String prefix) {
        List<String> suggestions = new ArrayList<>();
        for (String suggestion : commandService.suggestions(prefix)) {
            String permission = permissionFor(suggestion);
            if (sender.hasPermission("announcer.admin")
                    || permission == null
                    || sender.hasPermission(permission)) {
                suggestions.add(suggestion);
            }
        }
        if (sender.hasPermission("announcer.editor")
                && "editor".startsWith(prefix.toLowerCase(java.util.Locale.ROOT))) {
            suggestions.add("editor");
        }
        return suggestions;
    }

    private String permissionFor(String subcommand) {
        return switch (subcommand.toLowerCase(java.util.Locale.ROOT)) {
            case "debug" -> "announcer.debug";
            case "discord" -> "announcer.discord";
            case "editor" -> "announcer.editor";
            case "migrate" -> "announcer.migrate";
            case "preview" -> "announcer.preview";
            case "redis" -> "announcer.redis";
            case "reload" -> "announcer.reload";
            case "send" -> "announcer.send";
            case "toggle" -> "announcer.toggle";
            default -> "announcer.admin";
        };
    }

    private CommandRequest request(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            UUID id = player.getUniqueId();
            return CommandRequest.player(id.toString(), args);
        }
        return CommandRequest.console(args);
    }
}
