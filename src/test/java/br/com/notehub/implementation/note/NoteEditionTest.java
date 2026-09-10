package br.com.notehub.implementation.note;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NoteEditionTest extends AbstractNoteServiceTest {

    @Test
    void shouldUpdateNameAndFullName_whenChangingName() {
        when(repository.findById(note.getId())).thenReturn(Optional.of(note));

        service.changeName(owner.getId(), note.getId(), "new-name");

        assertThat(note.getName()).isEqualTo("new-name");
        assertThat(note.getFullName()).isEqualTo("owner/new-name");
        verify(repository).saveAndFlush(note);
    }

    @Test
    void shouldThrowAccessDenied_whenChangingNameOfNoteFromAnotherUser() {
        when(repository.findById(note.getId())).thenReturn(Optional.of(note));
        UUID someoneElseId = UUID.randomUUID();

        assertThatThrownBy(() -> service.changeName(someoneElseId, note.getId(), "new-name"))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(note.getName()).isEqualTo("old-name");
        assertThat(note.getFullName()).isEqualTo("owner/old-name");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void shouldUpdateNameAndFullName_whenEditingNote() {
        when(repository.findById(note.getId())).thenReturn(Optional.of(note));

        service.edit(owner.getId(), note.getId(), "new-name", "new description", null, true, true);

        assertThat(note.getName()).isEqualTo("new-name");
        assertThat(note.getFullName()).isEqualTo("owner/new-name");
        assertThat(note.getDescription()).isEqualTo("new description");
        assertThat(note.isClosed()).isTrue();
        assertThat(note.isHidden()).isTrue();
        verify(repository).saveAndFlush(note);
        verify(feeder).onNoteHidden(note.getId());
    }

    @Test
    void shouldThrowAccessDenied_whenEditingNoteOfAnotherUser() {
        when(repository.findById(note.getId())).thenReturn(Optional.of(note));
        UUID someoneElseId = UUID.randomUUID();

        assertThatThrownBy(() -> service.edit(someoneElseId, note.getId(), "new-name", "desc", null, false, false))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(note.getName()).isEqualTo("old-name");
        assertThat(note.getFullName()).isEqualTo("owner/old-name");
        verify(repository, never()).saveAndFlush(any());
        verify(feeder, never()).onNoteHidden(any());
    }

    @Test
    void shouldSetOrphanFullNameForUser_delegatingToRepository() {
        UUID userId = UUID.randomUUID();

        service.setOrphanFullNameForUser(userId);

        verify(repository).setOrphanFullNameForUser(userId);
    }

}