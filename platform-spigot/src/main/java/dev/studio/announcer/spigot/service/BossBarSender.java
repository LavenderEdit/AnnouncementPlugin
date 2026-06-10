package dev.studio.announcer.spigot.service;

import net.kyori.adventure.bossbar.BossBar;

interface BossBarSender {

    void show(String audienceId, BossBar bossBar);

    void hide(String audienceId, BossBar bossBar);
}
