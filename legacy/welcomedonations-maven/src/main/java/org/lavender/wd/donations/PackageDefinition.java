package org.lavender.wd.donations;

/**
 *
 * @author Studios TKOH!
 */
public record PackageDefinition(
        String id,
        PackageType type,
        String displayLabel,
        String rankName,
        String broadcastFormat,
        boolean autoForwardProxy,
        String giveCommand) {

    public enum PackageType {
        DONATION,
        RANK
    }

    public boolean hasGiveCommand() {
        return giveCommand != null && !giveCommand.isBlank();
    }
}
