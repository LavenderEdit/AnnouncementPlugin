package dev.studio.announcer.api.service;

public interface ScheduledTask extends AutoCloseable {

    String id();

    boolean cancelled();

    @Override
    void close();
}
