package dev.studio.announcer.spigot.bootstrap;

import java.util.logging.Logger;

public final class StartupBanner {

    private StartupBanner() {
    }

    private static final String[] STUDIOS_ART = {
            " _____ _  _  ___  _    ___  ____  ___  . ____  ___  ",
            "/ ____| || || __|| |  / __||  _ \\| __|.|  _ \\| __| ",
            "\\___  \\  // || _| |__\\__ \\|  _/| _|..|  _/| _|    ",
            " ____/ \\_// |___|____|___/|_|  |___|.|_|  |___|    "
    };

    private static final int BOX_WIDTH = 60;

    public static void printEnable(Logger logger, String version, int announcementCount,
            boolean redisEnabled, boolean discordEnabled, boolean foliaReady) {
        String top = "+" + repeat("=", BOX_WIDTH) + "+";
        String sep = "+" + repeat("-", BOX_WIDTH) + "+";
        String empty = "|" + repeat(" ", BOX_WIDTH) + "|";

        logger.info("");
        logger.info(top);
        logger.info(empty);
        logger.info("|          AdvancedAnnouncer   v" + padRight(version, 24) + "  |");
        logger.info("|          Modern Announcement Engine                      |");
        logger.info(empty);
        logger.info(sep);
        logger.info(empty);
        for (String line : STUDIOS_ART) {
            logger.info("|   " + padRight(line, BOX_WIDTH - 3) + "|");
        }
        logger.info(empty);
        logger.info(sep);
        logger.info(empty);
        logger.info("|   Announcements : " + padRight(String.valueOf(announcementCount), 38) + "|");
        logger.info("|   Redis         : " + padRight(statusLabel(redisEnabled), 38) + "|");
        logger.info("|   Discord       : " + padRight(statusLabel(discordEnabled), 38) + "|");
        logger.info("|   Folia         : " + padRight(statusLabel(foliaReady), 38) + "|");
        logger.info("|   Platform      : " + padRight("Paper/Spigot 1.21.x", 38) + "|");
        logger.info(empty);
        logger.info(top);
        logger.info("");
    }

    public static void printDisable(Logger logger, String version) {
        String top = "+" + repeat("=", BOX_WIDTH) + "+";
        logger.info("");
        logger.info(top);
        logger.info("|   AdvancedAnnouncer  v" + padRight(version, 34) + "|");
        logger.info("|   Disabled successfully.                                 |");
        logger.info(top);
        logger.info("");
    }

    private static String statusLabel(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static String padRight(String value, int length) {
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }
}
