package dev.studio.announcer.spigot.service;

interface ToastSender {

    void addToast(String audienceId, VirtualToast toast);

    void grantToast(String audienceId, String toastKey);

    void removeToast(String audienceId, String toastKey);
}
