package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class EditorMenuHolder implements InventoryHolder {
    private final EditorSession session;
    private final EditorMenuType type;
    private final AnnouncementId announcementId;
    private final int page;
    private final Map<Integer, AnnouncementId> announcementSlots = new HashMap<>();
    private Inventory inventory;

    EditorMenuHolder(EditorSession session, EditorMenuType type) {
        this(session, type, null, 0);
    }

    EditorMenuHolder(EditorSession session, EditorMenuType type, AnnouncementId announcementId, int page) {
        this.session = session;
        this.type = type;
        this.announcementId = announcementId;
        this.page = page;
    }

    EditorSession session() {
        return session;
    }

    EditorMenuType type() {
        return type;
    }

    Optional<AnnouncementId> announcementId() {
        return Optional.ofNullable(announcementId);
    }

    int page() {
        return page;
    }

    void mapAnnouncement(int slot, AnnouncementId id) {
        announcementSlots.put(slot, id);
    }

    Optional<AnnouncementId> announcementAt(int slot) {
        return Optional.ofNullable(announcementSlots.get(slot));
    }

    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
