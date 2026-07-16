package dev.studio.announcer.application.command;

import java.util.Arrays;
import java.util.List;

public record CommandRequest(String senderId, boolean player, List<String> arguments) {

    public CommandRequest {
        senderId = senderId == null || senderId.isBlank() ? "console" : senderId.trim();
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }

    public static CommandRequest console(String... arguments) {
        return new CommandRequest("console", false, Arrays.asList(arguments));
    }

    public static CommandRequest player(String senderId, String... arguments) {
        return new CommandRequest(senderId, true, Arrays.asList(arguments));
    }

    public String argument(int index) {
        if (index < 0 || index >= arguments.size()) {
            throw new IndexOutOfBoundsException(
                    "Argument index " + index + " out of range (size=" + arguments.size() + ")");
        }
        return arguments.get(index);
    }
}
