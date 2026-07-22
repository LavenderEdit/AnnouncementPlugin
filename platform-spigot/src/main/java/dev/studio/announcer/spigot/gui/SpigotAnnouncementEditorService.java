package dev.studio.announcer.spigot.gui;

import dev.studio.announcer.api.service.AnnouncementDispatcher;
import dev.studio.announcer.api.service.AnnouncementEditorService;
import dev.studio.announcer.api.service.AnnouncementRepository;
import dev.studio.announcer.api.service.AnnouncementSchedulerService;
import dev.studio.announcer.api.service.DeliverySummary;
import dev.studio.announcer.api.service.SchedulerPort;
import dev.studio.announcer.application.usecase.ReloadConfigurationUseCase;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementChannel;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpigotAnnouncementEditorService implements AnnouncementEditorService, Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final AnnouncementRepository repository;
    private final AnnouncementDispatcher dispatcher;
    private final SchedulerPort scheduler;
    private final AnnouncementSchedulerService schedulerService;
    private final ReloadConfigurationUseCase reloadConfigurationUseCase;
    private final AnvilTextInputService inputService;
    private final MainEditorMenu mainMenu = new MainEditorMenu();
    private final AnnouncementListMenu listMenu = new AnnouncementListMenu();
    private final AnnouncementDetailMenu detailMenu = new AnnouncementDetailMenu();
    private final ConfirmDeleteMenu confirmDeleteMenu = new ConfirmDeleteMenu();
    private final SchedulerMenu schedulerMenu = new SchedulerMenu();
    private final EventsMenu eventsMenu = new EventsMenu();
    private final StylesMenu stylesMenu = new StylesMenu();
    private final MessageListMenu messageListMenu = new MessageListMenu();
    private final Map<UUID, EditorSession> sessions = new ConcurrentHashMap<>();

    public SpigotAnnouncementEditorService(
            JavaPlugin plugin,
            AnnouncementRepository repository,
            AnnouncementDispatcher dispatcher,
            SchedulerPort scheduler,
            AnnouncementSchedulerService schedulerService,
            ReloadConfigurationUseCase reloadConfigurationUseCase,
            AnvilTextInputService inputService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.schedulerService = Objects.requireNonNull(schedulerService, "schedulerService");
        this.reloadConfigurationUseCase = Objects.requireNonNull(reloadConfigurationUseCase, "reloadConfigurationUseCase");
        this.inputService = Objects.requireNonNull(inputService, "inputService");
    }

    @Override
    public void openEditor(String audienceId) {
        UUID playerId;
        try {
            playerId = UUID.fromString(audienceId);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Cannot open editor for invalid audience id: " + audienceId);
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            plugin.getLogger().warning("Cannot open editor because player is not online: " + audienceId);
            return;
        }
        EditorSession session = new EditorSession(repository);
        sessions.put(playerId, session);
        mainMenu.open(player, session);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }
        boolean isRightClick = event.isRightClick();
        switch (holder.type()) {
            case MAIN -> handleMain(player, holder, slot);
            case LIST -> handleList(player, holder, slot);
            case DETAIL -> handleDetail(player, holder, slot);
            case CONFIRM_DELETE -> handleConfirmDelete(player, holder, slot);
            case SCHEDULER -> handleScheduler(player, holder, slot);
            case EVENTS -> handleEvents(player, holder, slot);
            case STYLES -> handleStyles(player, holder, slot);
            case MESSAGE_LIST -> handleMessageList(player, holder, slot, isRightClick);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EditorMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        EditorSession session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null && !session.cancelled()) {
            session.cancel();
        }
    }

    @Override
    public void close() {
        sessions.values().stream()
                .filter(session -> !session.cancelled())
                .forEach(EditorSession::cancel);
        sessions.clear();
        inputService.clearAll();
    }

    private void handleMain(Player player, EditorMenuHolder holder, int slot) {
        if (slot == 10) {
            next(player, () -> listMenu.open(player, holder.session(), 0));
            return;
        }
        if (slot == 11) {
            next(player, () -> schedulerMenu.open(player, holder.session(), 0));
            return;
        }
        if (slot == 13) {
            next(player, () -> eventsMenu.open(player, holder.session(), 0));
            return;
        }
        if (slot == 15) {
            next(player, () -> stylesMenu.open(player, holder.session(), 0));
            return;
        }
        if (slot == 22) {
            save(player, holder.session());
            return;
        }
        if (slot == 26) {
            cancel(player, holder.session());
        }
    }

    private void handleList(Player player, EditorMenuHolder holder, int slot) {
        holder.announcementAt(slot).ifPresent(id -> next(player, () -> detailMenu.open(player, holder.session(), id)));
        if (slot == 45 && holder.page() > 0) {
            next(player, () -> listMenu.open(player, holder.session(), holder.page() - 1));
        } else if (slot == 47) {
            Announcement announcement = defaultAnnouncement();
            holder.session().create(announcement);
            player.sendMessage("Draft creado: " + announcement.id().value());
            next(player, () -> detailMenu.open(player, holder.session(), announcement.id()));
        } else if (slot == 49) {
            next(player, () -> mainMenu.open(player, holder.session()));
        } else if (slot == 53) {
            next(player, () -> listMenu.open(player, holder.session(), holder.page() + 1));
        }
    }

    private void handleDetail(Player player, EditorMenuHolder holder, int slot) {
        AnnouncementId id = holder.announcementId()
                .orElseThrow(() -> new IllegalStateException("Detail menu missing announcement id."));
        if (slot == 10) {
            holder.session().toggle(id);
            Announcement toggled = holder.session().draft(id).orElse(null);
            if (toggled != null && toggled.enabled() && toggled.interval().isPresent()) {
                schedulerService.schedule(toggled);
            } else {
                schedulerService.cancel(id);
            }
            next(player, () -> detailMenu.open(player, holder.session(), id));
        } else if (slot == 11) {
            Announcement currentForRename = holder.session().draft(id).orElseThrow(() -> new IllegalStateException("Draft not found: " + id.value()));
            inputService.openRenameAnvil(player, id, currentForRename.name(), newName -> {
                holder.session().rename(id, newName);
                player.sendMessage("Nombre actualizado: " + newName);
                next(player, () -> detailMenu.open(player, holder.session(), id));
            });
        } else if (slot == 12) {
            preview(player, holder, id);
        } else if (slot == 13) {
            next(player, () -> messageListMenu.open(player, holder.session(), id, 0));
        } else if (slot == 14) {
            AnnouncementId copyId = nextCopyId(holder.session(), id);
            holder.session().duplicate(id, copyId);
            player.sendMessage("Draft duplicado: " + copyId.value());
            next(player, () -> detailMenu.open(player, holder.session(), copyId));
        } else if (slot == 15) {
            sendAnnouncement(player, holder, id);
        } else if (slot == 16) {
            next(player, () -> confirmDeleteMenu.open(player, holder.session(), id));
        } else if (slot == 22) {
            next(player, () -> listMenu.open(player, holder.session(), 0));
        }
    }

    private void handleConfirmDelete(Player player, EditorMenuHolder holder, int slot) {
        AnnouncementId id = holder.announcementId()
                .orElseThrow(() -> new IllegalStateException("Delete menu missing announcement id."));
        if (slot == 11) {
            holder.session().delete(id);
            schedulerService.cancel(id);
            player.sendMessage("Draft eliminado: " + id.value());
            next(player, () -> listMenu.open(player, holder.session(), 0));
        } else if (slot == 15) {
            next(player, () -> detailMenu.open(player, holder.session(), id));
        }
    }

    private void handleScheduler(Player player, EditorMenuHolder holder, int slot) {
        holder.announcementAt(slot).ifPresent(id -> next(player, () -> detailMenu.open(player, holder.session(), id)));
        if (slot == 45 && holder.page() > 0) {
            next(player, () -> schedulerMenu.open(player, holder.session(), holder.page() - 1));
        } else if (slot == 49) {
            next(player, () -> mainMenu.open(player, holder.session()));
        } else if (slot == 53) {
            next(player, () -> schedulerMenu.open(player, holder.session(), holder.page() + 1));
        }
    }

    private void handleEvents(Player player, EditorMenuHolder holder, int slot) {
        holder.announcementAt(slot).ifPresent(id -> next(player, () -> detailMenu.open(player, holder.session(), id)));
        if (slot == 45 && holder.page() > 0) {
            next(player, () -> eventsMenu.open(player, holder.session(), holder.page() - 1));
        } else if (slot == 49) {
            next(player, () -> mainMenu.open(player, holder.session()));
        } else if (slot == 53) {
            next(player, () -> eventsMenu.open(player, holder.session(), holder.page() + 1));
        }
    }

    private void handleStyles(Player player, EditorMenuHolder holder, int slot) {
        holder.announcementAt(slot).ifPresent(id -> next(player, () -> detailMenu.open(player, holder.session(), id)));
        if (slot == 45 && holder.page() > 0) {
            next(player, () -> stylesMenu.open(player, holder.session(), holder.page() - 1));
        } else if (slot == 49) {
            next(player, () -> mainMenu.open(player, holder.session()));
        } else if (slot == 53) {
            next(player, () -> stylesMenu.open(player, holder.session(), holder.page() + 1));
        }
    }

    private void handleMessageList(Player player, EditorMenuHolder holder, int slot, boolean isRightClick) {
        AnnouncementId id = holder.announcementId()
                .orElseThrow(() -> new IllegalStateException("Message list menu missing announcement id."));
        if (slot == 49) {
            next(player, () -> detailMenu.open(player, holder.session(), id));
            return;
        }
        if (slot == 47) {
            holder.session().addMessage(id, "<gray>Nuevo mensaje</gray>");
            player.sendMessage("Mensaje agregado.");
            next(player, () -> messageListMenu.open(player, holder.session(), id, holder.page()));
            return;
        }
        if (slot == 45 && holder.page() > 0) {
            next(player, () -> messageListMenu.open(player, holder.session(), id, holder.page() - 1));
            return;
        }
        if (slot == 53) {
            next(player, () -> messageListMenu.open(player, holder.session(), id, holder.page() + 1));
            return;
        }
        holder.announcementAt(slot).ifPresent(msgId -> {
            String value = msgId.value();
            int colonIndex = value.lastIndexOf(":msg:");
            if (colonIndex >= 0) {
                try {
                    int msgIndex = Integer.parseInt(value.substring(colonIndex + 5));
                    Announcement announcement = holder.session().draft(id)
                            .orElseThrow(() -> new IllegalArgumentException("Draft not found: " + id.value()));
                    if (msgIndex >= 0 && msgIndex < announcement.messages().size()) {
                        if (isRightClick) {
                            holder.session().removeMessage(id, msgIndex);
                            player.sendMessage("Mensaje #" + (msgIndex + 1) + " eliminado.");
                            next(player, () -> messageListMenu.open(player, holder.session(), id, holder.page()));
                        } else {
                            String currentMessage = announcement.messages().get(msgIndex);
                            inputService.openMessageEditAnvil(player, id, msgIndex, currentMessage, newMessage -> {
                                holder.session().updateMessage(id, msgIndex, newMessage);
                                player.sendMessage("Mensaje #" + (msgIndex + 1) + " actualizado.");
                                next(player, () -> messageListMenu.open(player, holder.session(), id, holder.page()));
                            });
                        }
                    }
                } catch (NumberFormatException ex) {
                    player.sendMessage("Error al parsear indice de mensaje.");
                }
            }
        });
    }

    private void save(Player player, EditorSession session) {
        session.save();
        sessions.remove(player.getUniqueId());
        schedulerService.reschedule(repository.findAll());
        var reload = reloadConfigurationUseCase.reload();
        if (reload.success()) {
            player.sendMessage("Cambios guardados y configuracion recargada.");
        } else {
            player.sendMessage("Cambios guardados, pero la recarga fallo: " + reload.message());
            reload.errors().forEach(player::sendMessage);
        }
        player.closeInventory();
    }

    private void cancel(Player player, EditorSession session) {
        session.cancel();
        sessions.remove(player.getUniqueId());
        player.sendMessage("Cambios descartados.");
        player.closeInventory();
    }

    private void preview(Player player, EditorMenuHolder holder, AnnouncementId id) {
        Announcement announcement = holder.session().draft(id)
                .orElseThrow(() -> new IllegalArgumentException("Draft announcement not found: " + id.value()));
        DeliverySummary summary = dispatcher.preview(announcement, player.getUniqueId().toString());
        player.sendMessage("Preview enviado a " + summary.delivered() + " audiencia(s).");
    }

    private void sendAnnouncement(Player player, EditorMenuHolder holder, AnnouncementId id) {
        Announcement announcement = holder.session().draft(id)
                .orElseThrow(() -> new IllegalArgumentException("Draft announcement not found: " + id.value()));
        DeliverySummary summary = dispatcher.broadcast(announcement);
        player.sendMessage("Anuncio enviado a " + summary.delivered() + " audiencia(s).");
    }

    private Announcement defaultAnnouncement() {
        AnnouncementId id = inputService.sanitizeAnnouncementId("gui_" + System.currentTimeMillis());
        String message = "<green>Nuevo anuncio " + id.value() + "</green>";
        var validation = inputService.validateMiniMessage(message);
        if (!validation.valid()) {
            throw new IllegalStateException(String.join("; ", validation.errors()));
        }
        return Announcement.builder(id, id.value())
                .channels(EnumSet.of(AnnouncementChannel.CHAT))
                .messages(java.util.List.of(message))
                .build();
    }

    private AnnouncementId nextCopyId(EditorSession session, AnnouncementId sourceId) {
        String base = sourceId.value() + "_copy";
        AnnouncementId candidate = AnnouncementId.of(base);
        int attempt = 2;
        while (session.draft(candidate).isPresent()) {
            candidate = AnnouncementId.of(base + "_" + attempt);
            attempt++;
        }
        return candidate;
    }

    private void next(Player player, Runnable task) {
        scheduler.scheduleOnce("editor-" + player.getUniqueId() + "-" + System.nanoTime(), Duration.ZERO, task);
    }
}
