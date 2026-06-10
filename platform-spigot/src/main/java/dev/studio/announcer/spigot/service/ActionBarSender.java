package dev.studio.announcer.spigot.service;

import net.kyori.adventure.text.Component;

interface ActionBarSender {

    void show(String audienceId, Component message);

    void clear(String audienceId);
}
