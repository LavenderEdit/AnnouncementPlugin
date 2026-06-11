package dev.studio.announcer.application.command;

import java.util.List;

public record CommandOutcome(CommandStatus status, List<String> messages) {

    public CommandOutcome {
        status = status == null ? CommandStatus.SUCCESS : status;
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static CommandOutcome success(String message) {
        return new CommandOutcome(CommandStatus.SUCCESS, List.of(message));
    }

    public static CommandOutcome success(List<String> messages) {
        return new CommandOutcome(CommandStatus.SUCCESS, messages);
    }

    public static CommandOutcome error(String message) {
        return new CommandOutcome(CommandStatus.ERROR, List.of(message));
    }

    public static CommandOutcome error(List<String> messages) {
        return new CommandOutcome(CommandStatus.ERROR, messages);
    }
}
