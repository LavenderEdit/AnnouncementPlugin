package dev.studio.announcer.spigot.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.studio.announcer.common.repository.InMemoryAnnouncementRepository;
import dev.studio.announcer.domain.announcement.Announcement;
import dev.studio.announcer.domain.announcement.AnnouncementId;
import java.util.List;
import org.junit.jupiter.api.Test;

class EditorSessionTest {

    @Test
    void changesStayDraftUntilSave() {
        InMemoryAnnouncementRepository repository = new InMemoryAnnouncementRepository();
        repository.save(announcement("sale"));
        EditorSession session = new EditorSession(repository);

        session.toggle(AnnouncementId.of("sale"));

        assertTrue(repository.findById(AnnouncementId.of("sale")).orElseThrow().enabled());

        session.save();

        assertFalse(repository.findById(AnnouncementId.of("sale")).orElseThrow().enabled());
    }

    @Test
    void cancelDiscardsDraftChanges() {
        InMemoryAnnouncementRepository repository = new InMemoryAnnouncementRepository();
        repository.save(announcement("sale"));
        EditorSession session = new EditorSession(repository);

        session.delete(AnnouncementId.of("sale"));
        session.cancel();

        assertTrue(repository.findById(AnnouncementId.of("sale")).isPresent());
    }

    @Test
    void duplicateCreatesCopyWithNewIdOnSave() {
        InMemoryAnnouncementRepository repository = new InMemoryAnnouncementRepository();
        repository.save(announcement("sale"));
        EditorSession session = new EditorSession(repository);

        session.duplicate(AnnouncementId.of("sale"), AnnouncementId.of("sale_copy"));
        session.save();

        assertEquals(List.of("sale", "sale_copy"), repository.findAll().stream()
                .map(item -> item.id().value())
                .sorted()
                .toList());
    }

    private Announcement announcement(String id) {
        return Announcement.builder(AnnouncementId.of(id), id)
                .messages(List.of("<green>" + id + "</green>"))
                .build();
    }
}
