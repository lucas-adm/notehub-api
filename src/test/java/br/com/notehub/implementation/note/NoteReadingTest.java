package br.com.notehub.implementation.note;

import br.com.notehub.application.dto.response.note.DetailNoteRES;
import br.com.notehub.domain.note.Note;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NoteReadingTest extends AbstractNoteServiceTest {

    @Test
    void shouldReturnNote_whenFoundByLiveUsernameAndNameJoin() {
        when(repository.findByUserUsernameAndName("owner", "old-name")).thenReturn(Optional.of(note));

        DetailNoteRES result = service.getNote(null, "owner", "old-name");

        assertThat(result.name()).isEqualTo("old-name");
        verify(repository, never()).findByFullName(any());
    }

    @Test
    void shouldFallBackToFullName_whenLiveJoinFindsNothing_orphanedNote() {
        Note orphanNote = new Note(owner, "orphan-note", null, "# md", false, false, null);
        orphanNote.setId(UUID.randomUUID());
        orphanNote.setUser(null);
        orphanNote.setFullName(orphanNote.getId().toString().substring(0, 8) + "/orphan-note");

        when(repository.findByUserUsernameAndName("a1b2c3d4", "orphan-note")).thenReturn(Optional.empty());
        when(repository.findByFullName("a1b2c3d4/orphan-note")).thenReturn(Optional.of(orphanNote));

        DetailNoteRES result = service.getNote(null, "a1b2c3d4", "orphan-note");

        assertThat(result.name()).isEqualTo("orphan-note");
        assertThat(result.user()).isNull();
    }

    @Test
    void shouldThrowEntityNotFoundException_whenNoteNotFoundInEitherLookup() {
        when(repository.findByUserUsernameAndName("ghost", "missing")).thenReturn(Optional.empty());
        when(repository.findByFullName("ghost/missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNote(null, "ghost", "missing"))
                .isInstanceOf(EntityNotFoundException.class);
    }

}