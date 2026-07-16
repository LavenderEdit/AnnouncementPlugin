package dev.studio.announcer.spigot.bootstrap;

import java.util.logging.Logger;

public final class StartupBanner {

    private StartupBanner() {
    }

    private static final String[] LOGO_ART = {
            " ███  ████  █   █      ███  █   █ █   █  ███  █   █ █   █  ███  █████ ████    ",
            "█ ░░█ █░░░█ █░  █░    █ ░░█ ██  █░██  █░█ ░░█ █░  █░██  █░█ ░░░ █░░░░░█░░░█  ",
            "█████░█░░░█░█░░ █░░   █████░█░█ █░█░█ █░█░ ░█░█░░ █░█░█ █░█░ ░░░████░░████░░  ",
            "█░░░█░█░░ █░░█░█ ░░█  █░░░█░█░░██░█░░██░█░░ █░█░░ █░█░░██░█░░   █░░░░ █░░█░ ░ ",
            "█░░░█░████ ░░ █ ░ ░█░ █░░░█░█░░ █░█░░ █░░███ ░░███ ░█░░ █░░███  █████░█░░░█░  ",
            " ░░  ░░░░░░ ░  ░ ░  ░░ ░░  ░░░░  ░░░░  ░░ ░░░ ░ ░░░ ░░░  ░░ ░░░  ░░░░░ ░░  ░  ",
            "  ░   ░ ░░░░    ░    ░  ░   ░ ░   ░ ░   ░  ░░░   ░░░  ░   ░  ░░░  ░░░░░ ░   ░  "
    };

    private static final String[] STUDIOS_TKOH_ART = {
            "*******************************************************####################################%%%%%%%%%%%%%%%%%%%%%%%%%%",
            "*********************************************************##################################%%%%%%%%%%%%%%%%%%%%%%%%%%",
            "**********************************************************################################%%%%%%%%%%%%%%%%%%%%%%%%%%%",
            "***********************************************************###################################%%%%%%%%%%%%%%%%%%%%%%%",
            "***********************************************************########################################%%%%%%%%%%%%%%%%%%",
            "###****-            +*++************************************############################################%%%%%%%%%%%%%",
            "###***+.             .............-************:.-****++==++++*#**#####+*##*+++#######*######::=%######*::+#%%%%%%%%",
            "####**+.                             :+*****= -+*+. +**** -*###+.-##### -##= ##*=: +##+ ###=.-##*-.*##=.=##+ :#%%%%%%",
            "#####*+.  :**********++++++++++++++.  -*****- =********** -***#+.:##### -##= #####=.+#+ ##: ######= ##:.+##########%#",
            "#####*+.  =*********+++++++++++++++-  :+++***++:  .=***** -***#+.:##### -##= ###### =#+ ##:.######= *###*. ..+#######",
            "######+.  =***+:    .=+:              :+++++==****= -**** -*****:.##### -##- #####:.*#+ ##- +#####- ##++####+ +######",
            "######+   =**:   ...=+:....   ........-++++*- .=+=..***** -*****=..++=..*##- ++=: .###+ ###+.:**= .###- -**= -#######",
            "######+.  =**.  =+*+++++++=  .+++++-..-+++++++++++*****************++**#*##*****#######*#####****########***#########",
            "######+.  =**:   :---=++++=  .+++++-  :+++++--------------+*+---+*****+----+####*=-::--=#######+--=#######====*######",
            "######+.  =***+.       :++=  .+++++-  :+++++.             -*+   -****-   .**#*=           +####-   *######:   +######",
            "######+.  =*******++=.  -+=  .+++++-  :++++++****-   =******+   -**-    =**#*.   -*###*:   :###-   *######:   +######",
            "#######*************+.  -+=  :+++++-  :++++******-   =******+   -*.   -****#:   -#######:   =##-   *######:   +######",
            "######+.               :++=  :+++++-  :++++******-   =******+        :*****#.   #########   -##-              +######",
            "######+.   ...  ....-+++++=..:+++++-  :+*********-   =******+         .***##.   ########*   -##-   -------.   +######",
            "######+.  =**************+++++++++*-  :+*********-   =******+   .**=    =###+   .*#####*    *##-   *######:   +######",
            "#######-                              -**********-   +******+   -****.   -###*     -+:    .####-   *######:   +######",
            "########*.                          .+***********-   +******+   -*###*:   .*###+         *#####-   *######:   +####%#",
            "%%%%%%%%###############**************************************************#################################################%%##",
            "%%%%%%%%%#############********************************************##########################################%%%%%%%%%%%%%%%",
            "%%%%%%%%%%##############*************************************################################%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%",
            "%%%%%%%%%%%################*****************************###################################%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%",
            "%%%%%%%%%%%%####################**************######################################%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
    };

    private static final int BOX_WIDTH = 125;

    public static void printEnable(Logger logger, String version, int announcementCount,
            boolean redisEnabled, boolean discordEnabled, boolean foliaReady) {
        String top = "+" + repeat("=", BOX_WIDTH) + "+";
        String sep = "+" + repeat("-", BOX_WIDTH) + "+";
        String empty = "|" + repeat(" ", BOX_WIDTH) + "|";

        logger.info("");
        logger.info(top);
        logger.info(empty);
        int logoWidth = maxLineWidth(LOGO_ART);
        int logoPad = (BOX_WIDTH - logoWidth) / 2;
        for (String line : LOGO_ART) {
            logger.info("|" + repeat(" ", logoPad) + padRight(line, BOX_WIDTH - logoPad) + "|");
        }
        logger.info(empty);
        logger.info(sep);
        logger.info(empty);
        int studioWidth = maxLineWidth(STUDIOS_TKOH_ART);
        int studioPad = (BOX_WIDTH - studioWidth) / 2;
        for (String line : STUDIOS_TKOH_ART) {
            logger.info("|" + repeat(" ", studioPad) + padRight(line, BOX_WIDTH - studioPad) + "|");
        }
        logger.info(empty);
        logger.info(sep);
        logger.info(empty);
        logger.info("|  " + padRight("Version      : " + version, BOX_WIDTH - 2) + "|");
        logger.info("|  " + padRight("Announcements: " + announcementCount + " loaded", BOX_WIDTH - 2) + "|");
        logger.info("|  " + padRight("Platform     : Paper/Spigot 1.21.x", BOX_WIDTH - 2) + "|");
        logger.info(empty);
        logger.info(sep);
        logger.info(empty);
        logger.info("|  " + padRight("Redis   : " + statusLabel(redisEnabled), BOX_WIDTH - 2) + "|");
        logger.info("|  " + padRight("Discord : " + statusLabel(discordEnabled), BOX_WIDTH - 2) + "|");
        logger.info("|  " + padRight("Folia   : " + statusLabel(foliaReady), BOX_WIDTH - 2) + "|");
        logger.info(empty);
        logger.info(top);
        logger.info("");
        logger.info("  Type /announcer help to get started.");
        logger.info("");
    }

    public static void printDisable(Logger logger, String version) {
        String top = "+" + repeat("=", BOX_WIDTH) + "+";
        logger.info("");
        logger.info(top);
        logger.info("|  " + padRight("AdvancedAnnouncer v" + version, BOX_WIDTH - 2) + "|");
        logger.info("|  " + padRight("Disabled successfully.", BOX_WIDTH - 2) + "|");
        logger.info(top);
        logger.info("");
    }

    private static String statusLabel(boolean enabled) {
        return enabled ? "ON " : "OFF";
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

    private static int maxLineWidth(String[] lines) {
        int max = 0;
        for (String line : lines) {
            if (line.length() > max) {
                max = line.length();
            }
        }
        return max;
    }
}
